package org.elevenetc.playground.paas.runtime

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("FunctionRuntime")

fun main() {
    logger.info("Starting function runtime server on port 8080")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        post("/execute") {
            logger.info("=== EXECUTION STARTED ===")
            try {
                // USER_FUNCTION_CALL will be replaced with actual function call
                USER_FUNCTION_CALL
                logger.info("=== EXECUTION COMPLETED SUCCESSFULLY ===")
                call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
            } catch (e: Exception) {
                logger.error("=== EXECUTION FAILED ===", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("status" to "error", "message" to e.message)
                )
            }
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }
    }
}
