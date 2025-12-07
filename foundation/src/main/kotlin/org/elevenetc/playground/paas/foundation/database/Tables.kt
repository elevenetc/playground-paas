package org.elevenetc.playground.paas.foundation.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ProjectsTable : Table("projects") {
    val id = varchar("id", 128)
    val name = varchar("name", 256).uniqueIndex()
    val description = text("description").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object FunctionsTable : Table("functions") {
    val id = varchar("id", 128)
    val projectId = varchar("project_id", 128).references(ProjectsTable.id)
    val name = varchar("name", 256)
    val sourceCode = text("source_code")
    val returnType = varchar("return_type", 128)
    val parameters = text("parameters") // JSON string: [{"name": "a", "type": "Int"}]
    val status = varchar("status", 50)
    val containerName = varchar("container_name", 256).nullable() // Docker container name
    val containerId = varchar("container_id", 256).nullable() // Docker container ID
    val port = integer("port").nullable()
    val imageTag = varchar("image_tag", 256).nullable()
    val errorMessage = text("error_message").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object FunctionStatusHistoryTable : Table("function_status_history") {
    val id = varchar("id", 128)
    val functionId = varchar("function_id", 128).references(FunctionsTable.id, onDelete = ReferenceOption.CASCADE)
    val fromStatus = varchar("from_status", 50).nullable() // null for initial creation
    val toStatus = varchar("to_status", 50)
    val transitionTime = timestamp("transition_time")
    val durationMs = long("duration_ms").nullable() // Time spent in fromStatus
    val metadata = text("metadata").nullable() // JSON for extra context (errors, etc)

    override val primaryKey = PrimaryKey(id)
}
