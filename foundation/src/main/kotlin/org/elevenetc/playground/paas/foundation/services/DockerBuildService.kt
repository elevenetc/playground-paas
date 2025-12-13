package org.elevenetc.playground.paas.foundation.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.elevenetc.playground.paas.foundation.tools.Docker
import org.elevenetc.playground.paas.foundation.tools.Kind
import org.elevenetc.playground.paas.foundation.utils.copyDirectory
import org.elevenetc.playground.paas.foundation.utils.generateContainerName
import java.io.File

class DockerBuildService(
    val kind: Kind,
    val docker: Docker
) {
    private val templateDir = File("foundation/docker-template")
    private val buildDir = File("foundation/build/functions")

    init {
        buildDir.mkdirs()
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

                val result = docker.build(imageName, functionBuildDir)

                if (result.exitCode == 0) {
                    val loaded = kind.loadImageIntoKind(imageName)
                    if (loaded) {
                        BuildResult.Success(imageName, result.output)
                    } else {
                        BuildResult.Failure("Docker build succeeded but failed to load image into Kind cluster")
                    }
                } else {
                    BuildResult.Failure("Docker build failed: ${result.output}")
                }
            } catch (e: Exception) {
                BuildResult.Failure("Build error: ${e.message}")
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