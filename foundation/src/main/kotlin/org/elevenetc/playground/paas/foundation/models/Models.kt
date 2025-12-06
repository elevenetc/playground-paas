package org.elevenetc.playground.paas.foundation.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class App(
    val id: String,
    val name: String,
    val gitUrl: String? = null,
    val branch: String = "main",
    val env: Map<String, String> = emptyMap(),
    val status: AppStatus,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

@Serializable
enum class AppStatus {
    PENDING,
    BUILDING,
    RUNNING,
    STOPPED,
    FAILED
}

@Serializable
data class Deployment(
    val id: String,
    val appId: String,
    val imageTag: String,
    val replicas: Int = 1,
    val status: DeploymentStatus,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

@Serializable
enum class DeploymentStatus {
    PENDING,
    DEPLOYING,
    RUNNING,
    SCALING,
    FAILED
}

@Serializable
data class Container(
    val id: String,
    val deploymentId: String,
    val appId: String,
    val containerId: String? = null, // Docker container ID
    val port: Int? = null,
    val status: ContainerStatus,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

@Serializable
enum class ContainerStatus {
    PENDING,
    CREATING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED
}

// Request/Response DTOs
@Serializable
data class CreateAppRequest(
    val name: String,
    val gitUrl: String? = null,
    val branch: String = "main",
    val env: Map<String, String> = emptyMap()
)

@Serializable
data class UpdateAppRequest(
    val gitUrl: String? = null,
    val branch: String? = null,
    val env: Map<String, String>? = null
)

@Serializable
data class ScaleRequest(
    val replicas: Int
)
