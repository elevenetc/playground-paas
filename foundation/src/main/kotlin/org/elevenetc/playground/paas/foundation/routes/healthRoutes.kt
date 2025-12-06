package org.elevenetc.playground.paas.foundation.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.elevenetc.playground.paas.foundation.services.DockerService

fun Route.healthRoutes(dockerService: DockerService) {
    route("/health") {
        get {
            val dockerHealthy = dockerService.ping()

            val status = if (dockerHealthy) {
                mapOf(
                    "status" to "healthy",
                    "docker" to "connected"
                )
            } else {
                mapOf(
                    "status" to "degraded",
                    "docker" to "disconnected"
                )
            }

            call.respond(
                if (dockerHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                status
            )
        }
    }
}
