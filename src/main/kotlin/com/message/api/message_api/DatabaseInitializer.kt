package com.message.api.message_api

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class DatabaseInitializer(
    private val databaseFactory: DatabaseFactory,
    private val secretManagerService: SecretManagerService
) {

    @PostConstruct
    fun start() {
        val props = loadEnvFile("env/env.dev")

        val projectId = getConfig(props, "PROJECT_ID")
        val secretId  = getConfig(props, "DATABASE_PASSWORD_SECRET_ID")

        // Resolve password from Secret Manager, fall back to DATABASE_PASSWORD in env.dev / env var
        val password = try {
            secretManagerService.loadSecret(projectId, secretId)
        } catch (_: Exception) {
            getConfig(props, "DATABASE_PASSWORD")
        }

        val config = DatabaseConfig(
            name     = getConfig(props, "DATABASE_NAME"),
            username = getConfig(props, "DATABASE_USER_NAME"),
            password = password,
            url      = getConfig(props, "DATABASE_URL")
        )

        databaseFactory.init(config)

        // Auto-create tables if they don't exist
        transaction(databaseFactory.getDatabase()) {
            SchemaUtils.create(MessageTable)
        }
    }

    @PreDestroy
    fun stop() {
        databaseFactory.close()
    }

    /**
     * Lookup order:
     *  1. env.dev file (local dev)
     *  2. System environment variable (Docker / K8s)
     *  3. JVM system property (-D flags)
     */
    private fun getConfig(props: Properties, key: String): String =
        props.getProperty(key)
            ?: System.getenv(key)
            ?: System.getProperty(key)
            ?: throw IllegalStateException("Missing required configuration: $key")

    private fun loadEnvFile(path: String): Properties {
        val props = Properties()
        val filePath = Paths.get(path)
        if (!Files.exists(filePath)) return props   // not present in K8s – fall through to env vars
        Files.newBufferedReader(filePath).use { reader ->
            reader.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                val idx = trimmed.indexOf('=')
                if (idx > 0) {
                    props.setProperty(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim())
                }
            }
        }
        return props
    }
}