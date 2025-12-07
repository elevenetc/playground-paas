package org.elevenetc.playground.paas.foundation.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: Dotenv) {
        val database = Database.connect(createHikariDataSource(config))

        transaction(database) {
            SchemaUtils.create(ProjectsTable, FunctionsTable, FunctionStatusHistoryTable)
        }
    }

    private fun createHikariDataSource(config: Dotenv): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = config["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/paas"
            username = config["DATABASE_USER"] ?: "paas"
            password = config["DATABASE_PASSWORD"] ?: "paas"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }
}
