package com.message.api.message_api

import org.jetbrains.exposed.sql.Table

/**
 * Exposed table mapping for the "message_table" table:
 * id (auto-increment PK), productname, message.
 */
object MessageTable : Table("message_table") {
    val id = long("id").autoIncrement()
    val productName = varchar("productname", 255)
    val content = varchar("message", 4000)

    override val primaryKey = PrimaryKey(id)
}
