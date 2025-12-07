package org.elevenetc.playground.paas.foundation.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.elevenetc.playground.paas.foundation.database.FunctionsTable
import org.elevenetc.playground.paas.foundation.models.Function
import org.elevenetc.playground.paas.foundation.models.FunctionParameter
import org.elevenetc.playground.paas.foundation.models.FunctionStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class FunctionRepository(
    private val statusHistoryRepository: FunctionStatusHistoryRepository
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun create(
        projectId: String,
        name: String,
        sourceCode: String,
        returnType: String,
        parameters: List<FunctionParameter>
    ): Function {
        return transaction {
            val id = UUID.randomUUID().toString()
            val now = Instant.now()
            val pendingStatus = FunctionStatus.PENDING

            FunctionsTable.insert {
                it[FunctionsTable.id] = id
                it[FunctionsTable.projectId] = projectId
                it[FunctionsTable.name] = name
                it[FunctionsTable.sourceCode] = sourceCode
                it[FunctionsTable.returnType] = returnType
                it[FunctionsTable.parameters] = json.encodeToString(parameters)
                it[status] = pendingStatus.name
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Record initial status transition
            statusHistoryRepository.recordTransitionIfChanged(
                functionId = id,
                prevStatus = null,
                toStatus = pendingStatus
            )

            Function(
                id = id,
                projectId = projectId,
                name = name,
                sourceCode = sourceCode,
                returnType = returnType,
                parameters = parameters,
                status = pendingStatus,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    fun findAll(): List<Function> {
        return transaction {
            FunctionsTable.selectAll().map { rowToFunction(it) }
        }
    }

    fun findById(id: String): Function? {
        return transaction {
            FunctionsTable.selectAll()
                .where { FunctionsTable.id eq id }
                .map { rowToFunction(it) }
                .singleOrNull()
        }
    }

    fun findByProjectId(projectId: String): List<Function> {
        return transaction {
            FunctionsTable.selectAll()
                .where { FunctionsTable.projectId eq projectId }
                .map { rowToFunction(it) }
        }
    }

    fun update(
        id: String,
        name: String?,
        sourceCode: String?,
        returnType: String?,
        parameters: List<FunctionParameter>?
    ): Function? {
        return transaction {
            // Check if function exists
            if (findById(id) == null) return@transaction null

            FunctionsTable.update({ FunctionsTable.id eq id }) {
                if (name != null) it[FunctionsTable.name] = name
                if (sourceCode != null) {
                    it[FunctionsTable.sourceCode] = sourceCode
                    // Reset status when code changes
                    it[status] = FunctionStatus.PENDING.name
                }
                if (returnType != null) it[FunctionsTable.returnType] = returnType
                if (parameters != null) it[FunctionsTable.parameters] = json.encodeToString(parameters)
                it[updatedAt] = Instant.now()
            }

            findById(id)
        }
    }

    /**
     * Updates function status with automatic history tracking.
     * @param errorMessage Optional error message to store in function and history metadata
     */
    fun updateStatus(id: String, status: FunctionStatus, errorMessage: String? = null): Function? {
        return transaction {
            // Get current status directly from the database without nested transaction
            val oldStatus = FunctionsTable
                .selectAll()
                .where { FunctionsTable.id eq id }
                .map { FunctionStatus.valueOf(it[FunctionsTable.status]) }
                .singleOrNull() ?: return@transaction null

            // Update status and optionally error message
            FunctionsTable.update({ FunctionsTable.id eq id }) {
                it[FunctionsTable.status] = status.name
                it[FunctionsTable.errorMessage] = errorMessage
                it[updatedAt] = Instant.now()
            }

            statusHistoryRepository.recordTransitionIfChanged(
                functionId = id,
                prevStatus = oldStatus,
                toStatus = status,
                metadata = if (errorMessage != null) mapOf("error" to errorMessage) else null
            )

            findById(id)
        }
    }

    fun updateContainerInfo(
        functionId: String,
        containerName: String,
        containerId: String,
        port: Int,
        imageTag: String,
        status: FunctionStatus
    ): Function? {
        return transaction {
            val prevStatus = FunctionsTable
                .selectAll()
                .where { FunctionsTable.id eq functionId }
                .map { FunctionStatus.valueOf(it[FunctionsTable.status]) }
                .singleOrNull() ?: return@transaction null

            FunctionsTable.update({ FunctionsTable.id eq functionId }) {
                it[FunctionsTable.containerName] = containerName
                it[FunctionsTable.containerId] = containerId
                it[FunctionsTable.port] = port
                it[FunctionsTable.imageTag] = imageTag
                it[FunctionsTable.status] = status.name
                it[updatedAt] = Instant.now()
            }

            statusHistoryRepository.recordTransitionIfChanged(
                functionId = functionId,
                prevStatus = prevStatus,
                toStatus = status
            )

            findById(functionId)
        }
    }

    fun delete(id: String): Boolean {
        return transaction {
            FunctionsTable.deleteWhere { FunctionsTable.id eq id } > 0
        }
    }

    private fun rowToFunction(row: ResultRow): Function {
        val parametersJson = row[FunctionsTable.parameters]
        val parameters = json.decodeFromString<List<FunctionParameter>>(parametersJson)

        return Function(
            id = row[FunctionsTable.id],
            projectId = row[FunctionsTable.projectId],
            name = row[FunctionsTable.name],
            sourceCode = row[FunctionsTable.sourceCode],
            returnType = row[FunctionsTable.returnType],
            parameters = parameters,
            status = FunctionStatus.valueOf(row[FunctionsTable.status]),
            containerName = row[FunctionsTable.containerName],
            containerId = row[FunctionsTable.containerId],
            port = row[FunctionsTable.port],
            imageTag = row[FunctionsTable.imageTag],
            errorMessage = row[FunctionsTable.errorMessage],
            createdAt = row[FunctionsTable.createdAt],
            updatedAt = row[FunctionsTable.updatedAt]
        )
    }
}
