package org.elevenetc.playground.paas.foundation

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.elevenetc.playground.paas.foundation.database.DatabaseFactory
import org.elevenetc.playground.paas.foundation.repositories.FunctionRepository
import org.elevenetc.playground.paas.foundation.repositories.ProjectRepository
import org.elevenetc.playground.paas.foundation.routes.functionRoutes
import org.elevenetc.playground.paas.foundation.routes.healthRoutes
import org.elevenetc.playground.paas.foundation.routes.projectRoutes
import org.elevenetc.playground.paas.foundation.services.DockerBuildService
import org.elevenetc.playground.paas.foundation.services.DockerService
import org.elevenetc.playground.paas.foundation.services.FunctionService
import org.elevenetc.playground.paas.foundation.services.ProjectService

fun main() {
    val appConfig = dotenv {
        directory = "./"
        ignoreIfMissing = true
    }

    embeddedServer(
        Netty,
        port = appConfig["PORT"]?.toIntOrNull() ?: 8080,
        host = "0.0.0.0",
        module = {
            this.module(appConfig)
        }
    ).start(wait = true)
}

fun Application.module(appConfig: Dotenv) {
    // Initialize database
    DatabaseFactory.init(appConfig)

    // Initialize repositories
    val projectRepository = ProjectRepository()
    val functionRepository = FunctionRepository()

    // Initialize services
    val dockerService = DockerService(appConfig)
    val dockerBuildService = DockerBuildService()
    val projectService = ProjectService(projectRepository)
    val functionService = FunctionService(functionRepository, dockerBuildService, projectService)

    // Configure plugins
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // For development - restrict this in production
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Logging is handled by logback configuration

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Internal server error"))
            )
        }
    }

    // Configure routing
    routing {
        get("/") {
            call.respondText("Mini-PaaS Foundation API", ContentType.Text.Plain)
        }

        healthRoutes(dockerService)
        projectRoutes(projectService)
        functionRoutes(functionService, projectService)
    }

    // Shutdown hook
    environment.monitor.subscribe(ApplicationStopped) {
        dockerService.close()
    }
}
