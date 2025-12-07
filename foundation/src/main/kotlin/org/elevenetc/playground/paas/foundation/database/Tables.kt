package org.elevenetc.playground.paas.foundation.database

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
