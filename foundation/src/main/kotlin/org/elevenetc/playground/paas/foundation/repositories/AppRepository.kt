package org.elevenetc.playground.paas.foundation.repositories

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.elevenetc.playground.paas.foundation.database.AppsTable
import org.elevenetc.playground.paas.foundation.models.App
import org.elevenetc.playground.paas.foundation.models.AppStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class AppRepository {
    fun create(name: String, gitUrl: String?, branch: String, env: Map<String, String>): App {
        return transaction {
            val now = Instant.now()
            val id = UUID.randomUUID().toString()

            AppsTable.insert {
                it[AppsTable.id] = id
                it[AppsTable.name] = name
                it[AppsTable.gitUrl] = gitUrl
                it[AppsTable.branch] = branch
                it[AppsTable.env] = Json.encodeToString(env)
                it[AppsTable.status] = AppStatus.PENDING.name
                it[createdAt] = now
                it[updatedAt] = now
            }

            App(
                id = id,
                name = name,
                gitUrl = gitUrl,
                branch = branch,
                env = env,
                status = AppStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    fun findById(id: String): App? {
        return transaction {
            AppsTable.selectAll().where { AppsTable.id eq id }
                .map { it.toApp() }
                .singleOrNull()
        }
    }

    fun findAll(): List<App> {
        return transaction {
            AppsTable.selectAll().map { it.toApp() }
        }
    }

    fun update(id: String, gitUrl: String? = null, branch: String? = null, env: Map<String, String>? = null): App? {
        return transaction {
            val existing = findById(id) ?: return@transaction null

            AppsTable.update({ AppsTable.id eq id }) {
                if (gitUrl != null) it[AppsTable.gitUrl] = gitUrl
                if (branch != null) it[AppsTable.branch] = branch
                if (env != null) it[AppsTable.env] = Json.encodeToString(env)
                it[updatedAt] = Instant.now()
            }

            findById(id)
        }
    }

    fun updateStatus(id: String, status: AppStatus): Boolean {
        return transaction {
            AppsTable.update({ AppsTable.id eq id }) {
                it[AppsTable.status] = status.name
                it[updatedAt] = Instant.now()
            } > 0
        }
    }

    fun delete(id: String): Boolean {
        return transaction {
            AppsTable.deleteWhere { AppsTable.id.eq(id) } > 0
        }
    }

    private fun ResultRow.toApp() = App(
        id = this[AppsTable.id],
        name = this[AppsTable.name],
        gitUrl = this[AppsTable.gitUrl],
        branch = this[AppsTable.branch],
        env = Json.decodeFromString(this[AppsTable.env]),
        status = AppStatus.valueOf(this[AppsTable.status]),
        createdAt = this[AppsTable.createdAt],
        updatedAt = this[AppsTable.updatedAt]
    )
}
