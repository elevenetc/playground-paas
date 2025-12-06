package org.elevenetc.playground.paas.foundation.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

@Serializable
data class Function(
    val id: String,
    val projectId: String,
    val name: String,
    val sourceCode: String,
    val returnType: String,
    val parameters: List<FunctionParameter>,
    val status: FunctionStatus,
    val containerId: String? = null,
    val port: Int? = null,
    val imageTag: String? = null,
    val errorMessage: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

@Serializable
data class FunctionParameter(
    val name: String,
    val type: String
)

@Serializable
enum class FunctionStatus {
    PENDING,
    COMPILING,
    READY,
    FAILED,
    STOPPED
}

// Request DTOs
@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class CreateFunctionRequest(
    val name: String? = null,
    val sourceCode: String
)

@Serializable
data class UpdateFunctionRequest(
    val name: String? = null,
    val sourceCode: String? = null
)

@Serializable
data class ExecuteFunctionRequest(
    val arguments: Map<String, String> // parameter name -> value as string
)

@Serializable
data class ExecuteFunctionResponse(
    val result: String,
    val executionTimeMs: Long
)
