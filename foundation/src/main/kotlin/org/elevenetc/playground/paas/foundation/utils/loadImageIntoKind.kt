package org.elevenetc.playground.paas.foundation.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Loads a Docker image into the Kind cluster.
 * This is necessary because Kind runs in its own Docker container and doesn't have access to the host's images.
 */
internal suspend fun loadImageIntoKind(imageName: String, clusterName: String = "paas-cluster"): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("kind", "load", "docker-image", imageName, "--name", clusterName)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                true
            } else {
                println("Failed to load image into Kind: $output")
                false
            }
        } catch (e: Exception) {
            println("Error loading image into Kind: ${e.message}")
            false
        }
    }
}