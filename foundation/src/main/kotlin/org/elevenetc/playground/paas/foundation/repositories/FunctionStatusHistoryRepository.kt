package org.elevenetc.playground.paas.foundation.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.elevenetc.playground.paas.foundation.database.FunctionStatusHistoryTable
import org.elevenetc.playground.paas.foundation.models.FunctionStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class FunctionStatusHistoryRepository {

    /**
     * Records a status transition with automatic duration calculation.
     * Skips recording if fromStatus equals toStatus (no actual transition).
     */
    fun recordTransitionIfChanged(
        functionId: String,
        prevStatus: FunctionStatus?,
        toStatus: FunctionStatus,
        metadata: Map<String, Any>? = null
    ) {
        if (prevStatus == toStatus) {
            return
        }

        transaction {
            val lastTransition = getLastTransition(functionId)
            val durationMs = lastTransition?.let {
                Instant.now().toEpochMilli() - it.transitionTime.toEpochMilli()
            }

            FunctionStatusHistoryTable.insert { history ->
                history[FunctionStatusHistoryTable.id] = UUID.randomUUID().toString()
                history[FunctionStatusHistoryTable.functionId] = functionId
                history[FunctionStatusHistoryTable.fromStatus] = prevStatus?.name
                history[FunctionStatusHistoryTable.toStatus] = toStatus.name
                history[transitionTime] = Instant.now()
                history[FunctionStatusHistoryTable.durationMs] = durationMs
                history[FunctionStatusHistoryTable.metadata] = metadata?.let { m ->
                    Json.encodeToString<Map<String, String>>(m.mapValues { it.value.toString() })
                }
            }
        }
    }

    private fun getLastTransition(functionId: String): StatusTransition? {
        return transaction {
            FunctionStatusHistoryTable
                .selectAll()
                .where { FunctionStatusHistoryTable.functionId eq functionId }
                .orderBy(FunctionStatusHistoryTable.transitionTime to org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(1)
                .map { it.toStatusTransition() }
                .firstOrNull()
        }
    }

    private fun ResultRow.toStatusTransition() = StatusTransition(
        id = this[FunctionStatusHistoryTable.id],
        functionId = this[FunctionStatusHistoryTable.functionId],
        fromStatus = this[FunctionStatusHistoryTable.fromStatus].let { status ->
            if (status == null) null else FunctionStatus.valueOf(status)
        },
        toStatus = FunctionStatus.valueOf(this[FunctionStatusHistoryTable.toStatus]),
        transitionTime = this[FunctionStatusHistoryTable.transitionTime],
        durationMs = this[FunctionStatusHistoryTable.durationMs],
        metadata = this[FunctionStatusHistoryTable.metadata]?.let { Json.decodeFromString<Map<String, String>>(it) }
    )
}

data class StatusTransition(
    val id: String,
    val functionId: String,
    val fromStatus: FunctionStatus?,
    val toStatus: FunctionStatus,
    val transitionTime: Instant,
    val durationMs: Long?,
    val metadata: Map<String, String>?
)
