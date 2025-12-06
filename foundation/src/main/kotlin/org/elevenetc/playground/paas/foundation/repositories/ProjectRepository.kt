package org.elevenetc.playground.paas.foundation.repositories

import org.elevenetc.playground.paas.foundation.database.ProjectsTable
import org.elevenetc.playground.paas.foundation.models.Project
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class ProjectRepository {

    fun create(name: String, description: String?): Project {
        return transaction {
            val id = UUID.randomUUID().toString()
            val now = Instant.now()

            ProjectsTable.insert {
                it[ProjectsTable.id] = id
                it[ProjectsTable.name] = name
                it[ProjectsTable.description] = description
                it[createdAt] = now
                it[updatedAt] = now
            }

            Project(
                id = id,
                name = name,
                description = description,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    fun findAll(): List<Project> {
        return transaction {
            ProjectsTable.selectAll().map { rowToProject(it) }
        }
    }

    fun findById(id: String): Project? {
        return transaction {
            ProjectsTable.selectAll()
                .where { ProjectsTable.id eq id }
                .map { rowToProject(it) }
                .singleOrNull()
        }
    }

    fun findByName(name: String): Project? {
        return transaction {
            ProjectsTable.selectAll()
                .where { ProjectsTable.name eq name }
                .map { rowToProject(it) }
                .singleOrNull()
        }
    }

    fun update(id: String, name: String?, description: String?): Project? {
        return transaction {
            val existing = findById(id) ?: return@transaction null

            ProjectsTable.update({ ProjectsTable.id eq id }) {
                if (name != null) it[ProjectsTable.name] = name
                if (description != null) it[ProjectsTable.description] = description
                it[updatedAt] = Instant.now()
            }

            findById(id)
        }
    }

    fun delete(id: String): Boolean {
        return transaction {
            ProjectsTable.deleteWhere { ProjectsTable.id eq id } > 0
        }
    }

    private fun rowToProject(row: ResultRow): Project {
        return Project(
            id = row[ProjectsTable.id],
            name = row[ProjectsTable.name],
            description = row[ProjectsTable.description],
            createdAt = row[ProjectsTable.createdAt],
            updatedAt = row[ProjectsTable.updatedAt]
        )
    }
}
