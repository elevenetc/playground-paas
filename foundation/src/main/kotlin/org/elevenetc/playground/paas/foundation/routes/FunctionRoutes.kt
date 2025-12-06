package org.elevenetc.playground.paas.foundation.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.elevenetc.playground.paas.foundation.models.CreateFunctionRequest
import org.elevenetc.playground.paas.foundation.models.UpdateFunctionRequest
import org.elevenetc.playground.paas.foundation.services.FunctionService
import org.elevenetc.playground.paas.foundation.services.ProjectService

fun Route.functionRoutes(functionService: FunctionService, projectService: ProjectService) {
    route("/api/projects/{projectId}/functions") {
        // Create function
        post {
            val projectId = call.parameters["projectId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing project ID")
            )

            // Verify project exists
            if (!projectService.projectExists(projectId)) {
                return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Project not found")
                )
            }

            val request = call.receive<CreateFunctionRequest>()

            val function = functionService.createFunction(projectId, request)

            call.respond(HttpStatusCode.Created, function)
        }

        // List all functions for a project
        get {
            val projectId = call.parameters["projectId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing project ID")
            )

            val functions = functionService.getFunctionsByProjectId(projectId)
            call.respond(functions)
        }

        // Get function by ID
        get("/{functionId}") {
            val functionId = call.parameters["functionId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing function ID")
            )

            val function = functionService.getFunctionById(functionId)
            if (function != null) {
                call.respond(function)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Function not found"))
            }
        }

        // Update function
        put("/{functionId}") {
            val functionId = call.parameters["functionId"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing function ID")
            )

            val request = call.receive<UpdateFunctionRequest>()

            val function = functionService.updateFunction(functionId, request)

            if (function != null) {
                call.respond(function)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Function not found"))
            }
        }

        // Delete function
        delete("/{functionId}") {
            val functionId = call.parameters["functionId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing function ID")
            )

            val deleted = functionService.deleteFunction(functionId)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Function not found"))
            }
        }
    }

    // Alternative route for getting all functions (not scoped by project)
    route("/api/functions") {
        get {
            val functions = functionService.getAllFunctions()
            call.respond(functions)
        }

        get("/{functionId}") {
            val functionId = call.parameters["functionId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing function ID")
            )

            val function = functionService.getFunctionById(functionId)
            if (function != null) {
                call.respond(function)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Function not found"))
            }
        }
    }
}
