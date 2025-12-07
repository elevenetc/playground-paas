package org.elevenetc.playground.paas.foundation.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.elevenetc.playground.paas.foundation.compiler.extractFunctionName
import org.elevenetc.playground.paas.foundation.compiler.extractParameters
import org.elevenetc.playground.paas.foundation.compiler.extractReturnType
import org.elevenetc.playground.paas.foundation.models.CreateFunctionRequest
import org.elevenetc.playground.paas.foundation.models.Function
import org.elevenetc.playground.paas.foundation.models.FunctionStatus
import org.elevenetc.playground.paas.foundation.models.UpdateFunctionRequest
import org.elevenetc.playground.paas.foundation.repositories.FunctionRepository

class FunctionService(
    private val functionRepository: FunctionRepository,
    private val dockerBuildService: DockerBuildService,
    private val projectService: ProjectService
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 5000 // 5 second timeout
        }
        expectSuccess = false // Don't throw on non-2xx status codes
    }

    fun createFunction(projectId: String, request: CreateFunctionRequest): Function {
        // Extract name, parameters, and return type from source code
        val name = request.name ?: extractFunctionName(request.sourceCode)
        val parameters = extractParameters(request.sourceCode)
        val returnType = extractReturnType(request.sourceCode)

        val function = functionRepository.create(
            projectId = projectId,
            name = name,
            sourceCode = request.sourceCode,
            returnType = returnType,
            parameters = parameters
        )

        // Trigger Docker build asynchronously
        scope.launch {
            buildAndDeployFunction(function)
        }

        return function
    }

    private suspend fun buildAndDeployFunction(function: Function) {
        try {
            // Update status to COMPILING
            functionRepository.updateStatus(function.id, FunctionStatus.COMPILING)

            // Get project info for naming
            val project = projectService.getProjectById(function.projectId)
            val projectName = project?.name?.lowercase()?.replace(Regex("[^a-z0-9-]"), "-") ?: "unknown"

            // Build Docker image
            when (val buildResult = dockerBuildService.buildFunctionImage(
                functionId = function.id,
                projectName = projectName,
                functionName = function.name,
                sourceCode = function.sourceCode
            )) {
                is BuildResult.Success -> {
                    // Run container
                    when (val containerResult = dockerBuildService.runContainer(
                        imageName = buildResult.imageName,
                        projectName = projectName,
                        functionName = function.name
                    )) {
                        is ContainerResult.Success -> {
                            // Update function with container info
                            functionRepository.updateContainerInfo(
                                functionId = function.id,
                                containerName = containerResult.containerName,
                                containerId = containerResult.containerId,
                                port = containerResult.port,
                                imageTag = buildResult.imageName,
                                status = FunctionStatus.READY
                            )
                        }

                        is ContainerResult.Failure -> {
                            functionRepository.updateError(function.id, containerResult.error)
                        }
                    }
                }

                is BuildResult.Failure -> {
                    functionRepository.updateError(function.id, buildResult.error)
                }
            }
        } catch (e: Exception) {
            functionRepository.updateError(function.id, "Build error: ${e.message}")
        }
    }

    fun getAllFunctions(): List<Function> {
        return functionRepository.findAll()
    }

    fun getFunctionById(id: String): Function? {
        return functionRepository.findById(id)
    }

    fun getFunctionsByProjectId(projectId: String): List<Function> {
        return functionRepository.findByProjectId(projectId)
    }

    fun updateFunction(id: String, request: UpdateFunctionRequest): Function? {
        // If source code is being updated, extract parameters and return type
        val parameters = if (request.sourceCode != null) {
            extractParameters(request.sourceCode)
        } else null

        val returnType = if (request.sourceCode != null) {
            extractReturnType(request.sourceCode)
        } else null

        return functionRepository.update(
            id = id,
            name = request.name,
            sourceCode = request.sourceCode,
            returnType = returnType,
            parameters = parameters
        )
    }

    fun deleteFunction(id: String): Boolean {
        // Get function info before deleting
        val function = functionRepository.findById(id) ?: return false

        // If function has a container, set state to DELETING and clean it up
        if (function.containerName != null) {
            // Set status to DELETING
            functionRepository.updateStatus(id, FunctionStatus.DELETING)

            // Stop and remove container asynchronously
            scope.launch {
                cleanupAndDeleteFunction(id, function.containerName)
            }

            // Return true - deletion is in progress
            return true
        }

        // No container, safe to delete immediately
        return functionRepository.delete(id)
    }

    private suspend fun cleanupAndDeleteFunction(functionId: String, containerName: String) {
        try {
            // Attempt to stop and remove container
            when (val result = dockerBuildService.stopAndRemoveContainer(containerName)) {
                is ContainerDeletionResult.Success -> {
                    // Successfully cleaned up, now delete from DB
                    functionRepository.delete(functionId)
                }

                is ContainerDeletionResult.Failure -> {
                    // Failed to cleanup container, update status with error
                    // Keep the function record so user can retry
                    functionRepository.updateStatus(functionId, FunctionStatus.FAILED)
                    functionRepository.updateError(functionId, "Container cleanup failed: ${result.error}")
                }
            }
        } catch (e: Exception) {
            // Unexpected error during cleanup
            functionRepository.updateStatus(functionId, FunctionStatus.FAILED)
            functionRepository.updateError(functionId, "Unexpected cleanup error: ${e.message}")
        }
    }

    suspend fun executeFunction(functionId: String): FunctionExecutionResult {
        // Get function info
        val function = functionRepository.findById(functionId)
            ?: return FunctionExecutionResult.NotFound

        // Check if function is ready
        if (function.status != FunctionStatus.READY) {
            return FunctionExecutionResult.NotReady("Function is ${function.status}, not READY")
        }

        // Check if we have port info
        val port = function.port
            ?: return FunctionExecutionResult.Error("Function container port not available")

        return try {
            // Make HTTP POST request to container's /execute endpoint
            val response: HttpResponse = httpClient.post("http://localhost:$port/execute") {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.ContentType, "application/json")
                }
            }

            // Check status code
            if (!response.status.isSuccess()) {
                return FunctionExecutionResult.Error("Execution failed with status ${response.status.value}: ${response.bodyAsText()}")
            }

            // Extract result from response body
            val responseBody = response.bodyAsText()
            // Parse JSON to get the result field
            val resultMatch = Regex(""""result"\s*:\s*"([^"]*)"""").find(responseBody)
            val result = resultMatch?.groupValues?.get(1)
                ?: return FunctionExecutionResult.Error("Failed to parse result from response: $responseBody")

            FunctionExecutionResult.Success(result)
        } catch (e: Exception) {
            FunctionExecutionResult.Error("Execution failed: ${e.message}")
        }
    }

    suspend fun getGeneratedApplicationSource(projectId: String, functionId: String): String? {
        // Get project name
        val project = projectService.getProjectById(projectId) ?: return null
        val projectName = project.name.lowercase().replace(Regex("[^a-z0-9-]"), "-")

        return dockerBuildService.getGeneratedApplicationSource(projectName, functionId)
    }

    suspend fun getGeneratedUserFunctionSource(projectId: String, functionId: String): String? {
        // Get project name
        val project = projectService.getProjectById(projectId) ?: return null
        val projectName = project.name.lowercase().replace(Regex("[^a-z0-9-]"), "-")

        return dockerBuildService.getGeneratedUserFunctionSource(projectName, functionId)
    }
}

sealed class FunctionExecutionResult {
    data class Success(val result: String) : FunctionExecutionResult()
    object NotFound : FunctionExecutionResult()
    data class NotReady(val message: String) : FunctionExecutionResult()
    data class Error(val message: String) : FunctionExecutionResult()
}