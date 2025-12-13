package org.elevenetc.playground.paas.foundation.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.elevenetc.playground.paas.foundation.models.FunctionStatus
import org.elevenetc.playground.paas.foundation.repositories.FunctionRepository
import org.elevenetc.playground.paas.foundation.utils.loadImageIntoKind
import org.slf4j.LoggerFactory

/**
 * Service responsible for reconciling Kubernetes state with database state.
 * On startup, it checks if all functions in the database have corresponding K8s resources,
 * and recreates them if missing.
 */
class KubernetesReconciliationService(
    private val functionRepository: FunctionRepository,
    private val kubernetesService: KubernetesService,
    private val projectService: ProjectService
) {
    private val logger = LoggerFactory.getLogger(KubernetesReconciliationService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Reconcile all functions on startup.
     * This should be called once when the application starts.
     */
    fun reconcileOnStartup() {
        scope.launch {
            try {
                logger.info("Starting Kubernetes reconciliation...")

                // Get all functions that should have K8s resources
                // (READY, or failed deployments that might have partial resources)
                val functions = functionRepository.findAll().filter { function ->
                    function.status == FunctionStatus.READY ||
                            function.status == FunctionStatus.DEPLOYMENT_FAILED ||
                            function.status == FunctionStatus.SERVICE_FAILED
                }

                if (functions.isEmpty()) {
                    logger.info("No functions to reconcile")
                    return@launch
                }

                logger.info("Found ${functions.size} function(s) to check for reconciliation")

                var reconciledCount = 0

                functions.forEach { function ->
                    try {
                        // Get project info for naming
                        val project = projectService.getProjectById(function.projectId)
                        if (project == null) {
                            logger.warn("Project ${function.projectId} not found for function ${function.id}, skipping reconciliation")
                            return@forEach
                        }

                        val projectName = project.name.lowercase().replace(Regex("[^a-z0-9-]"), "-")

                        // Check if deployment exists
                        val deploymentExists = kubernetesService.deploymentExists(projectName, function.name)

                        // Check if service exists
                        val serviceExists = kubernetesService.serviceExists(projectName, function.name)

                        // If both exist, nothing to do
                        if (deploymentExists && serviceExists) {
                            logger.debug("Function ${function.name} (${function.id}) already has K8s resources")
                            return@forEach
                        }

                        // Resources are missing, need to reconcile
                        logger.info("Reconciling function ${function.name} (${function.id}): deployment=$deploymentExists, service=$serviceExists")

                        reconcileFunction(function, projectName, deploymentExists, serviceExists)
                        reconciledCount++

                    } catch (e: Exception) {
                        logger.error("Failed to reconcile function ${function.name} (${function.id})", e)
                    }
                }

                logger.info("Kubernetes reconciliation completed. Reconciled $reconciledCount function(s)")

            } catch (e: Exception) {
                logger.error("Kubernetes reconciliation failed", e)
            }
        }
    }

    /**
     * Reconcile a single function by recreating missing K8s resources.
     */
    private suspend fun reconcileFunction(
        function: org.elevenetc.playground.paas.foundation.models.Function,
        projectName: String,
        deploymentExists: Boolean,
        serviceExists: Boolean
    ) {
        try {
            // Get the image name
            val imageName = function.imageTag
            if (imageName == null) {
                logger.warn("Function ${function.name} has no imageTag, cannot reconcile")
                return
            }

            // If deployment is missing, recreate it
            if (!deploymentExists) {
                logger.info("Recreating deployment for function ${function.name}")

                // First, ensure the image is loaded into Kind
                logger.info("Loading image $imageName into Kind cluster...")
                val imageLoaded = loadImageIntoKind(imageName)

                if (!imageLoaded) {
                    logger.error("Failed to load image $imageName into Kind cluster")
                    functionRepository.updateStatus(
                        function.id,
                        FunctionStatus.DEPLOYMENT_FAILED,
                        "Failed to load Docker image into Kind cluster"
                    )
                    return // Can't proceed without the image
                }

                logger.info("Image loaded successfully, creating deployment...")

                when (val result = kubernetesService.createDeployment(
                    imageName = imageName,
                    projectName = projectName,
                    functionName = function.name,
                    functionId = function.id
                )) {
                    is K8sDeploymentResult.Success -> {
                        logger.info("Successfully recreated deployment ${result.deploymentName}")

                        // Update database with deployment name
                        functionRepository.updateContainerInfo(
                            functionId = function.id,
                            containerName = result.deploymentName,
                            containerId = function.containerId ?: "", // Keep existing service name
                            port = function.port ?: 0,
                            imageTag = imageName,
                            status = if (serviceExists) FunctionStatus.READY else function.status
                        )
                    }

                    is K8sDeploymentResult.Failure -> {
                        logger.error("Failed to recreate deployment for ${function.name}: ${result.error}")
                        functionRepository.updateStatus(function.id, FunctionStatus.DEPLOYMENT_FAILED, result.error)
                        return // Don't try to create service if deployment failed
                    }
                }
            }

            // If service is missing, recreate it
            if (!serviceExists) {
                logger.info("Recreating service for function ${function.name}")

                when (val result = kubernetesService.createService(
                    projectName = projectName,
                    functionName = function.name,
                    functionId = function.id
                )) {
                    is K8sServiceResult.Success -> {
                        logger.info("Successfully recreated service ${result.serviceName} on NodePort ${result.nodePort}")

                        // Update database with service info
                        functionRepository.updateContainerInfo(
                            functionId = function.id,
                            containerName = function.containerName ?: "", // Keep existing deployment name
                            containerId = result.serviceName,
                            port = result.nodePort,
                            imageTag = imageName,
                            status = FunctionStatus.READY
                        )
                    }

                    is K8sServiceResult.Failure -> {
                        logger.error("Failed to recreate service for ${function.name}: ${result.error}")
                        functionRepository.updateStatus(function.id, FunctionStatus.SERVICE_FAILED, result.error)
                    }
                }
            }

            logger.info("Function ${function.name} reconciliation completed")

        } catch (e: Exception) {
            logger.error("Error during reconciliation of function ${function.name}", e)
        }
    }
}
