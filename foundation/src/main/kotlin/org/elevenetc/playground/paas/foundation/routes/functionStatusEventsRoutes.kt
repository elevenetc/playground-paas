package org.elevenetc.playground.paas.foundation.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.elevenetc.playground.paas.foundation.events.FunctionStatusEventBus

@Serializable
data class FunctionStatusEventDto(
    val functionId: String,
    val status: String,
    val errorMessage: String? = null
)

/**
 * Helper function to send SSE formatted message
 */
private suspend fun ByteWriteChannel.sendSseEvent(
    data: String,
    event: String? = null,
    id: String? = null
) {
    if (event != null) {
        writeStringUtf8("event: $event\n")
    }
    if (id != null) {
        writeStringUtf8("id: $id\n")
    }
    writeStringUtf8("data: $data\n\n")
    flush()
}

fun Route.functionStatusEventsRoutes(eventBus: FunctionStatusEventBus) {
    val json = Json { ignoreUnknownKeys = true }

    /**
     * SSE endpoint for streaming all function status updates
     * GET /functions/events
     *
     * Clients connect to this endpoint to receive real-time status updates for all functions.
     */
    get("/functions/events") {
        call.response.cacheControl(CacheControl.NoCache(null))
        call.response.header(HttpHeaders.Connection, "keep-alive")
        call.response.header(HttpHeaders.ContentType, "text/event-stream")

        call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
            // Send initial connection event
            sendSseEvent(
                data = json.encodeToString(
                    mapOf(
                        "type" to "connected",
                        "message" to "Connected to all function status updates"
                    )
                ),
                event = "connected"
            )

            // Subscribe to all events
            eventBus.events.collect { event ->
                val eventDto = FunctionStatusEventDto(
                    functionId = event.functionId,
                    status = event.status.name,
                    errorMessage = event.errorMessage
                )

                sendSseEvent(
                    data = json.encodeToString(eventDto),
                    event = "status",
                    id = "${event.functionId}-${System.currentTimeMillis()}"
                )
            }
        }
    }
}
