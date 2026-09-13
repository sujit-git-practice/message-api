package com.message.api.message_api

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.stereotype.Component
import java.io.Closeable
import java.sql.Connection
import java.time.Duration

@Component
class DatabaseFactory : Closeable {

    @Volatile
    private var initialized = false
    private lateinit var hikariDataSource: HikariDataSource
    private lateinit var database: Database

    fun getDatabase(): Database = database

    fun init(databaseConfig: DatabaseConfig): HikariDataSource {
        synchronized(this) {
            if (initialized) {

                return hikariDataSource
            }

            val jdbcUrl = databaseConfig.url
            val username = databaseConfig.username
            val password = databaseConfig.password

            hikariDataSource = createHikariDataSource(
                url = jdbcUrl,
                username = username,
                password = password

            )

            database = Database.connect(hikariDataSource)

            try {
                testConnection(hikariDataSource)
                initialized = true

            } catch (e: Exception) {

                close()
                throw e
            }
            return hikariDataSource
        }
    }

    private fun createHikariDataSource(
        url: String,
        username: String,
        password: String,
    ): HikariDataSource {


        val hikari = HikariConfig().apply {
            this.jdbcUrl = url
            this.username = username
            this.password = password
            this.driverClassName = "org.postgresql.Driver"

            this.minimumIdle = 5
            this.connectionTimeout = 30000
            this.idleTimeout = 600000
            this.maxLifetime = 1200000
            this.isAutoCommit = false

            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // NOTE: Do NOT extract socketFactory/cloudSqlInstance as separate properties.
            // PostgreSQL JDBC driver only recognizes them in the JDBC URL query string.
            // The full URL with all ?param=value pairs must be passed as-is to jdbcUrl.

            initializationFailTimeout = Duration.ofSeconds(30).toMillis()
            validate()
        }
        return HikariDataSource(hikari)
    }

    private fun extractCloudSqlInstance(url: String): String =
        "cloudSqlInstance=([^&]+)".toRegex().find(url)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("cloudSqlInstance not found in JDBC URL")


    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(
            Dispatchers.IO,
            db = database,
            transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
        ) { block() }
    }

    override fun close() {
        if (initialized && ::hikariDataSource.isInitialized) {

            hikariDataSource.close()
            initialized = false
        }
    }

    private fun testConnection(dataSource: HikariDataSource) {
        dataSource.connection.use { it.prepareStatement("SELECT 1").execute() }
    }
}

