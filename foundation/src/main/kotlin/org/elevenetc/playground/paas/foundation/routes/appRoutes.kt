package org.elevenetc.playground.paas.foundation.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.elevenetc.playground.paas.foundation.models.CreateAppRequest
import org.elevenetc.playground.paas.foundation.models.UpdateAppRequest
import org.elevenetc.playground.paas.foundation.repositories.AppRepository

fun Route.appRoutes(appRepository: AppRepository) {
    route("/api/apps") {
        // Create app
        post {
            val request = call.receive<CreateAppRequest>()

            val app = appRepository.create(
                name = request.name,
                gitUrl = request.gitUrl,
                branch = request.branch,
                env = request.env
            )

            call.respond(HttpStatusCode.Created, app)
        }

        // List all apps
        get {
            val apps = appRepository.findAll()
            call.respond(apps)
        }

        // Get app by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing app ID")
            )

            val app = appRepository.findById(id)
            if (app != null) {
                call.respond(app)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "App not found"))
            }
        }

        // Update app
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing app ID")
            )

            val request = call.receive<UpdateAppRequest>()

            val app = appRepository.update(
                id = id,
                gitUrl = request.gitUrl,
                branch = request.branch,
                env = request.env
            )

            if (app != null) {
                call.respond(app)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "App not found"))
            }
        }

        // Delete app
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing app ID")
            )

            val deleted = appRepository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "App not found"))
            }
        }
    }
}
