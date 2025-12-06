package org.elevenetc.playground.paas.foundation

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.elevenetc.playground.paas.foundation.database.FunctionsTable
import org.elevenetc.playground.paas.foundation.database.ProjectsTable
import org.elevenetc.playground.paas.foundation.repositories.FunctionRepository
import org.elevenetc.playground.paas.foundation.repositories.ProjectRepository
import org.elevenetc.playground.paas.foundation.routes.functionRoutes
import org.elevenetc.playground.paas.foundation.routes.projectRoutes
import org.elevenetc.playground.paas.foundation.services.FunctionService
import org.elevenetc.playground.paas.foundation.services.ProjectService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.testModule(resetDatabase: Boolean = true) {
    // Set up in-memory H2 database for testing
    val database = Database.connect(createTestDataSource())

    // Create and/or reset tables
    if (resetDatabase) {
        transaction(database) {
            SchemaUtils.drop(FunctionsTable, ProjectsTable)
            SchemaUtils.create(ProjectsTable, FunctionsTable)
        }
    } else {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ProjectsTable, FunctionsTable)
        }
    }

    // Initialize repositories
    val projectRepository = ProjectRepository()
    val functionRepository = FunctionRepository()

    // Initialize services
    val projectService = ProjectService(projectRepository)
    val functionService = FunctionService(functionRepository)

    // Configure plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

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
        projectRoutes(projectService)
        functionRoutes(functionService, projectService)
    }
}

private fun createTestDataSource(): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        driverClassName = "org.h2.Driver"
        jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        username = "sa"
        password = ""
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    return HikariDataSource(hikariConfig)
}

// Helper function to configure test application without module reloading
fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    environment {
        config = io.ktor.server.config.MapApplicationConfig()
    }
    application { testModule() }
    block()
}

// Helper function to make it easy to send JSON requests from strings
suspend fun HttpRequestBuilder.jsonBody(body: String) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

// Helper function to send serializable objects as JSON
inline fun <reified T> HttpRequestBuilder.jsonBody(body: T) {
    contentType(ContentType.Application.Json)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    setBody(json.encodeToString(serializer(), body))
}

// Helper function to get response body as string
suspend fun HttpResponse.bodyAsText(): String = bodyAsText()
