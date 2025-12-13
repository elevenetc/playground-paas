package org.elevenetc.playground.paas.foundation.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.elevenetc.playground.paas.foundation.utils.copyDirectory
import org.elevenetc.playground.paas.foundation.utils.loadImageIntoKind
import java.io.File

class DockerBuildService {
    private val templateDir = File("foundation/docker-template")
    private val buildDir = File("foundation/build/functions")

    init {
        buildDir.mkdirs()
    }

    /**
     * Generates standardized container/image names for functions.
     * Format: playground-paas-function-{project-name}-{function-name}
     */
    fun generateContainerName(projectName: String, functionName: String): String {
        val sanitizedProject = projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
        val sanitizedFunction = functionName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
        return "playground-paas-function-$sanitizedProject-$sanitizedFunction"
    }

    suspend fun buildFunctionImage(
        functionId: String,
        projectName: String,
        functionName: String,
        sourceCode: String
    ): BuildResult {
        return withContext(Dispatchers.IO) {
            try {
                // Create nested directory: build/functions/{projectName}/
                val projectBuildDir = File(buildDir, projectName)
                projectBuildDir.mkdirs()

                val functionBuildDir = File(projectBuildDir, functionId)

                // Clean up if exists
                if (functionBuildDir.exists()) {
                    functionBuildDir.deleteRecursively()
                }

                // Copy template to build directory
                copyDirectory(templateDir, functionBuildDir)

                // Inject user function source code
                val userFunctionFile = File(
                    functionBuildDir,
                    "src/main/kotlin/org/elevenetc/playground/paas/runtime/UserFunction.kt"
                )
                val userFunctionContent = userFunctionFile.readText()
                    .replace("USER_FUNCTION_SOURCE", sourceCode)
                userFunctionFile.writeText(userFunctionContent)

                // Inject function call in Application.kt
                val applicationFile = File(
                    functionBuildDir,
                    "src/main/kotlin/org/elevenetc/playground/paas/runtime/Application.kt"
                )

                val applicationContent = applicationFile.readText()
                    .replace("USER_FUNCTION_CALL", "$functionName()")
                applicationFile.writeText(applicationContent)

                // Build Docker image with centralized naming
                val imageName = generateContainerName(projectName, functionName)
                val processBuilder = ProcessBuilder(
                    "docker", "build", "-t", imageName, "."
                )
                processBuilder.directory(functionBuildDir)
                processBuilder.redirectErrorStream(true)

                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    val loaded = loadImageIntoKind(imageName)
                    if (loaded) {
                        BuildResult.Success(imageName, output)
                    } else {
                        BuildResult.Failure("Docker build succeeded but failed to load image into Kind cluster")
                    }
                } else {
                    BuildResult.Failure("Docker build failed: $output")
                }
            } catch (e: Exception) {
                BuildResult.Failure("Build error: ${e.message}")
            }
        }
    }

    suspend fun runContainer(
        imageName: String,
        projectName: String,
        functionName: String
    ): ContainerResult {
        return withContext(Dispatchers.IO) {
            try {
                val containerName = generateContainerName(projectName, functionName)

                // Stop and remove existing container if any
                ProcessBuilder("docker", "stop", containerName).start().waitFor()
                ProcessBuilder("docker", "rm", containerName).start().waitFor()

                // Run new container with random port mapping in playground-paas network
                // Add compose labels to group in Docker Desktop
                val processBuilder = ProcessBuilder(
                    "docker", "run", "-d",
                    "--name", containerName,
                    "--network", "playground-paas_default",
                    "--label", "com.docker.compose.project=playground-paas",
                    "--label", "com.docker.compose.service=$containerName",
                    "-p", "0:8080",  // Random host port
                    imageName
                )
                processBuilder.redirectErrorStream(true)

                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    return@withContext ContainerResult.Failure("Failed to start container: $output")
                }

                val containerId = output.trim()

                // Get the assigned port
                val portProcess = ProcessBuilder(
                    "docker", "port", containerName, "8080"
                ).start()
                val portOutput = portProcess.inputStream.bufferedReader().readText().trim()

                // Port output format: "0.0.0.0:54321" or "[::]:54321"
                val port = portOutput.split(":").lastOrNull()?.toIntOrNull()
                    ?: return@withContext ContainerResult.Failure("Could not determine container port")

                ContainerResult.Success(containerId, containerName, port)
            } catch (e: Exception) {
                ContainerResult.Failure("Container error: ${e.message}")
            }
        }
    }

    suspend fun stopAndRemoveContainer(containerName: String): ContainerDeletionResult {
        return withContext(Dispatchers.IO) {
            try {
                // Stop the container
                val stopProcess = ProcessBuilder("docker", "stop", containerName)
                stopProcess.redirectErrorStream(true)
                val stopProc = stopProcess.start()
                val stopOutput = stopProc.inputStream.bufferedReader().readText()
                val stopExitCode = stopProc.waitFor()

                if (stopExitCode != 0) {
                    return@withContext ContainerDeletionResult.Failure("Failed to stop container: $stopOutput")
                }

                // Remove the container
                val rmProcess = ProcessBuilder("docker", "rm", containerName)
                rmProcess.redirectErrorStream(true)
                val rmProc = rmProcess.start()
                val rmOutput = rmProc.inputStream.bufferedReader().readText()
                val rmExitCode = rmProc.waitFor()

                if (rmExitCode != 0) {
                    return@withContext ContainerDeletionResult.Failure("Failed to remove container: $rmOutput")
                }

                ContainerDeletionResult.Success
            } catch (e: Exception) {
                ContainerDeletionResult.Failure("Container deletion error: ${e.message}")
            }
        }
    }

    suspend fun getGeneratedApplicationSource(projectName: String, functionId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val projectBuildDir = File(buildDir, projectName)
                val functionBuildDir = File(projectBuildDir, functionId)
                val applicationFile = File(
                    functionBuildDir,
                    "src/main/kotlin/org/elevenetc/playground/paas/runtime/Application.kt"
                )

                if (applicationFile.exists()) {
                    applicationFile.readText()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getGeneratedUserFunctionSource(projectName: String, functionId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val projectBuildDir = File(buildDir, projectName)
                val functionBuildDir = File(projectBuildDir, functionId)
                val userFunctionFile = File(
                    functionBuildDir,
                    "src/main/kotlin/org/elevenetc/playground/paas/runtime/UserFunction.kt"
                )

                if (userFunctionFile.exists()) {
                    userFunctionFile.readText()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

sealed class BuildResult {
    data class Success(val imageName: String, val buildLog: String) : BuildResult()
    data class Failure(val error: String) : BuildResult()
}

sealed class ContainerResult {
    data class Success(val containerId: String, val containerName: String, val port: Int) : ContainerResult()
    data class Failure(val error: String) : ContainerResult()
}

sealed class ContainerDeletionResult {
    object Success : ContainerDeletionResult()
    data class Failure(val error: String) : ContainerDeletionResult()
}