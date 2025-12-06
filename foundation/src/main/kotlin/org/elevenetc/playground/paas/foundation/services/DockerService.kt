package org.elevenetc.playground.paas.foundation.services

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory

class DockerService(config: Dotenv) {
    private val logger = LoggerFactory.getLogger(DockerService::class.java)
    private val dockerClient: DockerClient

    init {
        val dockerHost = config["DOCKER_HOST"] ?: "unix:///var/run/docker.sock"

        val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerHost)
            .build()

        val httpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(dockerConfig.dockerHost)
            .build()

        dockerClient = DockerClientImpl.getInstance(dockerConfig, httpClient)

        logger.info("Docker service initialized with host: $dockerHost")

        try {
            val info = dockerClient.infoCmd().exec()
            logger.info("Connected to Docker daemon: ${info.name}, version: ${info.serverVersion}")
        } catch (e: Exception) {
            logger.error("Failed to connect to Docker daemon", e)
        }
    }

    fun listContainers(): List<Container> {
        return dockerClient.listContainersCmd().withShowAll(true).exec()
    }

    fun createContainer(
        imageName: String,
        containerName: String,
        env: List<String> = emptyList(),
        ports: Map<Int, Int> = emptyMap() // container port -> host port
    ): String {
        logger.info("Creating container: $containerName from image: $imageName")

        val createContainerCmd = dockerClient.createContainerCmd(imageName)
            .withName(containerName)
            .withEnv(env)

        // TODO: Add port bindings and other configurations

        val container = createContainerCmd.exec()
        logger.info("Container created: ${container.id}")

        return container.id
    }

    fun startContainer(containerId: String) {
        logger.info("Starting container: $containerId")
        dockerClient.startContainerCmd(containerId).exec()
    }

    fun stopContainer(containerId: String) {
        logger.info("Stopping container: $containerId")
        dockerClient.stopContainerCmd(containerId).exec()
    }

    fun removeContainer(containerId: String, force: Boolean = false) {
        logger.info("Removing container: $containerId")
        dockerClient.removeContainerCmd(containerId).withForce(force).exec()
    }

    fun inspectContainer(containerId: String): com.github.dockerjava.api.command.InspectContainerResponse {
        return dockerClient.inspectContainerCmd(containerId).exec()
    }

    fun getContainerLogs(containerId: String, tail: Int = 100): String {
        // This is a simplified version - in production, you'd want to stream logs
        logger.info("Getting logs for container: $containerId")
        // TODO: Implement proper log streaming
        return "Log streaming not yet implemented"
    }

    fun ping(): Boolean {
        return try {
            dockerClient.pingCmd().exec()
            true
        } catch (e: Exception) {
            logger.error("Docker ping failed", e)
            false
        }
    }

    fun close() {
        dockerClient.close()
    }
}
