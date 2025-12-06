package org.elevenetc.playground.paas.foundation.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.elevenetc.playground.paas.foundation.models.CreateProjectRequest
import org.elevenetc.playground.paas.foundation.models.UpdateProjectRequest
import org.elevenetc.playground.paas.foundation.services.ProjectService

fun Route.projectRoutes(projectService: ProjectService) {
    route("/api/projects") {
        post {
            val request = call.receive<CreateProjectRequest>()

            try {
                val project = projectService.createProject(request)
                call.respond(HttpStatusCode.Created, project)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to e.message)
                )
            }
        }

        get {
            val projects = projectService.getAllProjects()
            call.respond(projects)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing project ID")
            )

            val project = projectService.getProjectById(id)
            if (project != null) {
                call.respond(project)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
            }
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing project ID")
            )

            val request = call.receive<UpdateProjectRequest>()

            val project = projectService.updateProject(id, request)

            if (project != null) {
                call.respond(project)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing project ID")
            )

            val deleted = projectService.deleteProject(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
            }
        }
    }
}
