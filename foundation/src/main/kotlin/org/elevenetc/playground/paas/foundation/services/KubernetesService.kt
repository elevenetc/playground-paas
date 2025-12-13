package org.elevenetc.playground.paas.foundation.services

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.*
import io.kubernetes.client.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.elevenetc.playground.paas.foundation.utils.findAvailablePort
import org.elevenetc.playground.paas.foundation.utils.generateKubResourceName
import org.slf4j.LoggerFactory

class KubernetesService {
    private val logger = LoggerFactory.getLogger(KubernetesService::class.java)
    private val apiClient: ApiClient
    private val appsApi: AppsV1Api
    private val coreApi: CoreV1Api
    private val namespace = "default"

    init {
        try {
            // Configure Kubernetes client to use default config (~/.kube/config)
            apiClient = Config.defaultClient()
            Configuration.setDefaultApiClient(apiClient)

            appsApi = AppsV1Api(apiClient)
            coreApi = CoreV1Api(apiClient)

            logger.info("Kubernetes service initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Kubernetes client", e)
            throw e
        }
    }

    suspend fun createDeployment(
        imageName: String,
        projectName: String,
        functionName: String,
        functionId: String
    ): K8sDeploymentResult {
        return withContext(Dispatchers.IO) {
            try {
                val resourceName = generateKubResourceName(projectName, functionName)

                // Delete existing deployment if it exists
                try {
                    appsApi.deleteNamespacedDeployment(resourceName, namespace).execute()
                    logger.info("Deleted existing deployment: $resourceName")
                } catch (e: Exception) {
                    // Deployment doesn't exist, that's fine
                    logger.debug("No existing deployment to delete: $resourceName")
                }

                // Create deployment
                val deployment = V1Deployment()
                    .metadata(
                        V1ObjectMeta()
                            .name(resourceName)
                            .labels(
                                mapOf(
                                    "app" to "paas-function",
                                    "project" to projectName,
                                    "function" to functionName,
                                    "function-id" to functionId
                                )
                            )
                    )
                    .spec(
                        V1DeploymentSpec()
                            .replicas(1)
                            .selector(
                                V1LabelSelector()
                                    .matchLabels(mapOf("app" to resourceName))
                            )
                            .template(
                                V1PodTemplateSpec()
                                    .metadata(
                                        V1ObjectMeta()
                                            .labels(mapOf("app" to resourceName))
                                    )
                                    .spec(
                                        V1PodSpec()
                                            .containers(
                                                listOf(
                                                    V1Container()
                                                        .name("function")
                                                        .image(imageName)
                                                        .imagePullPolicy("Never") // Use local images loaded into Kind
                                                        .ports(
                                                            listOf(
                                                                V1ContainerPort()
                                                                    .containerPort(8080)
                                                                    .protocol("TCP")
                                                            )
                                                        )
                                                )
                                            )
                                    )
                            )
                    )

                appsApi.createNamespacedDeployment(namespace, deployment).execute()//add try
                logger.info("Deployment created, waiting for it to become ready: $resourceName")

                // Wait for deployment to be ready (with timeout)
                val isReady = waitForDeploymentReady(resourceName, timeoutSeconds = 60)

                if (isReady) {
                    logger.info("Deployment is ready: $resourceName")
                    K8sDeploymentResult.Success(resourceName)
                } else {
                    // Get failure reason from deployment status
                    val failureReason = getDeploymentFailureReason(resourceName)
                    logger.error("Deployment failed to become ready: $resourceName - $failureReason")
                    K8sDeploymentResult.Failure(failureReason)
                }
            } catch (e: Exception) {
                logger.error("Failed to create deployment", e)
                K8sDeploymentResult.Failure("Deployment error: ${e.message}")
            }
        }
    }

    suspend fun createService(
        projectName: String,
        functionName: String,
        functionId: String
    ): K8sServiceResult {
        return withContext(Dispatchers.IO) {
            try {
                val resourceName = generateKubResourceName(projectName, functionName)

                // Delete existing service if it exists
                try {
                    coreApi.deleteNamespacedService(resourceName, namespace).execute()
                    logger.info("Deleted existing service: $resourceName")
                } catch (e: Exception) {
                    // Service doesn't exist, that's fine
                    logger.debug("No existing service to delete: $resourceName")
                }

                // Create service (NodePort type for external access via Kind)
                val service = V1Service()
                    .metadata(
                        V1ObjectMeta()
                            .name(resourceName)
                            .labels(
                                mapOf(
                                    "app" to "paas-function",
                                    "project" to projectName,
                                    "function" to functionName,
                                    "function-id" to functionId
                                )
                            )
                    )
                    .spec(
                        V1ServiceSpec()
                            .type("NodePort")
                            .selector(mapOf("app" to resourceName))
                            .ports(
                                listOf(
                                    V1ServicePort()
                                        .name("http")
                                        .port(8080)
                                        .targetPort(io.kubernetes.client.custom.IntOrString(8080))
                                        .protocol("TCP")
                                )
                            )
                    )

                val createdService = coreApi.createNamespacedService(namespace, service).execute()

                // Get the NodePort assigned by Kubernetes
                val nodePort = createdService.spec?.ports?.firstOrNull()?.nodePort
                    ?: return@withContext K8sServiceResult.Failure("Could not determine NodePort")

                logger.info("Created service: $resourceName on NodePort: $nodePort")

                K8sServiceResult.Success(resourceName, nodePort)
            } catch (e: Exception) {
                logger.error("Failed to create service", e)
                K8sServiceResult.Failure("Service error: ${e.message}")
            }
        }
    }

    suspend fun deleteDeploymentAndService(
        projectName: String,
        functionName: String
    ): K8sDeletionResult {
        return withContext(Dispatchers.IO) {
            try {
                val resourceName = generateKubResourceName(projectName, functionName)
                var deploymentDeleted = false
                var serviceDeleted = false
                var deploymentError: String? = null
                var serviceError: String? = null

                // Delete deployment
                try {
                    appsApi.deleteNamespacedDeployment(resourceName, namespace).execute()
                    logger.info("Deleted deployment: $resourceName")
                    deploymentDeleted = true
                } catch (e: Exception) {
                    deploymentError = e.message
                    logger.warn("Failed to delete deployment: ${e.message}")
                }

                // Delete service
                try {
                    coreApi.deleteNamespacedService(resourceName, namespace).execute()
                    logger.info("Deleted service: $resourceName")
                    serviceDeleted = true
                } catch (e: Exception) {
                    serviceError = e.message
                    logger.warn("Failed to delete service: ${e.message}")
                }

                // Return appropriate result based on what succeeded/failed
                when {
                    deploymentDeleted && serviceDeleted -> K8sDeletionResult.Success
                    !deploymentDeleted && !serviceDeleted -> K8sDeletionResult.BothFailed(
                        deploymentError = deploymentError ?: "Unknown error",
                        serviceError = serviceError ?: "Unknown error"
                    )

                    !deploymentDeleted -> K8sDeletionResult.DeploymentFailed(
                        error = deploymentError ?: "Unknown error"
                    )

                    else -> K8sDeletionResult.ServiceFailed(
                        error = serviceError ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                logger.error("Unexpected error during deletion", e)
                K8sDeletionResult.UnexpectedError("Unexpected error: ${e.message}")
            }
        }
    }

    /**
     * Creates a port-forward to a service and returns a localhost URL.
     * The port-forward process is returned so it can be cleaned up after use.
     */
    suspend fun createPortForward(serviceName: String): PortForwardResult {
        return withContext(Dispatchers.IO) {
            try {
                // Find an available local port
                val localPort = findAvailablePort()

                // Start kubectl port-forward in the background
                val process = ProcessBuilder(
                    "kubectl", "port-forward",
                    "service/$serviceName",
                    "$localPort:8080",
                    "-n", namespace
                ).redirectErrorStream(true).start()

                // Wait a bit for port-forward to establish
                delay(1000)

                // Check if process is still alive
                if (!process.isAlive) {
                    val output = process.inputStream.bufferedReader().readText()
                    return@withContext PortForwardResult.Failure("Port-forward failed: $output")
                }

                PortForwardResult.Success(
                    url = "http://localhost:$localPort",
                    process = process
                )
            } catch (e: Exception) {
                logger.error("Failed to create port-forward", e)
                PortForwardResult.Failure("Port-forward error: ${e.message}")
            }
        }
    }

    /**
     * Waits for a deployment to be ready by polling its status.
     * Returns true if deployment becomes ready, false if timeout or failure.
     */
    private suspend fun waitForDeploymentReady(deploymentName: String, timeoutSeconds: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val timeoutMs = timeoutSeconds * 1000L

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val deployment = appsApi.readNamespacedDeployment(deploymentName, namespace).execute()
                    val status = deployment.status

                    if (status != null) {
                        // Check if deployment has available replicas
                        val availableReplicas = status.availableReplicas ?: 0
                        val desiredReplicas = deployment.spec?.replicas ?: 1

                        if (availableReplicas >= desiredReplicas) {
                            logger.debug("Deployment ready: $deploymentName ($availableReplicas/$desiredReplicas replicas available)")
                            return@withContext true
                        }

                        // Check for failure conditions
                        status.conditions?.forEach { condition ->
                            if (condition.type == "ReplicaFailure" && condition.status == "True") {
                                logger.warn("Deployment has ReplicaFailure: $deploymentName - ${condition.message}")
                                return@withContext false
                            }
                        }

                        logger.debug("Deployment not ready yet: $deploymentName ($availableReplicas/$desiredReplicas replicas)")
                    }

                    // Wait before polling again
                    delay(2000) // 2 seconds
                } catch (e: Exception) {
                    logger.warn("Error checking deployment status: ${e.message}")
                    delay(2000)
                }
            }

            logger.warn("Timeout waiting for deployment to be ready: $deploymentName")
            false
        }
    }

    /**
     * Gets the failure reason from a deployment's status.
     * Checks pod conditions and events to determine why deployment failed.
     */
    private suspend fun getDeploymentFailureReason(deploymentName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val deployment = appsApi.readNamespacedDeployment(deploymentName, namespace).execute()
                val status = deployment.status

                // Check deployment conditions
                status?.conditions?.forEach { condition ->
                    if (condition.status == "False" || condition.type == "ReplicaFailure") {
                        return@withContext "Deployment condition: ${condition.type} - ${condition.message ?: condition.reason ?: "Unknown"}"
                    }
                }

                // Try to get pod status for more details
                val labelSelector = "app=$deploymentName"
                val pods = coreApi.listNamespacedPod(namespace)
                    .labelSelector(labelSelector)
                    .execute()

                pods.items.firstOrNull()?.let { pod ->
                    // Check container statuses
                    pod.status?.containerStatuses?.forEach { containerStatus ->
                        containerStatus.state?.waiting?.let { waiting ->
                            return@withContext "Pod waiting: ${waiting.reason} - ${waiting.message ?: "No details"}"
                        }
                        containerStatus.state?.terminated?.let { terminated ->
                            return@withContext "Pod terminated: ${terminated.reason} - ${terminated.message ?: "Exit code ${terminated.exitCode}"}"
                        }
                    }

                    // Check pod conditions
                    pod.status?.conditions?.forEach { condition ->
                        if (condition.status == "False") {
                            return@withContext "Pod condition: ${condition.type} - ${condition.message ?: condition.reason ?: "Unknown"}"
                        }
                    }
                }

                "Deployment failed to become ready within timeout"
            } catch (e: Exception) {
                logger.error("Failed to get deployment failure reason", e)
                "Failed to determine failure reason: ${e.message}"
            }
        }
    }
}

sealed class K8sDeploymentResult {
    data class Success(val deploymentName: String) : K8sDeploymentResult()
    data class Failure(val error: String) : K8sDeploymentResult()
}

sealed class K8sServiceResult {
    data class Success(val serviceName: String, val nodePort: Int) : K8sServiceResult()
    data class Failure(val error: String) : K8sServiceResult()
}

sealed class K8sDeletionResult {
    object Success : K8sDeletionResult()
    data class DeploymentFailed(val error: String) : K8sDeletionResult()
    data class ServiceFailed(val error: String) : K8sDeletionResult()
    data class BothFailed(val deploymentError: String, val serviceError: String) : K8sDeletionResult()
    data class UnexpectedError(val error: String) : K8sDeletionResult()
}

sealed class PortForwardResult {
    data class Success(val url: String, val process: Process) : PortForwardResult()
    data class Failure(val error: String) : PortForwardResult()
}
