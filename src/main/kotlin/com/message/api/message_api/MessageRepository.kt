package com.message.api.message_api

import com.message.api.message_api.MessageTable.productName
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository

@Repository
class MessageRepository(private val databaseFactory: DatabaseFactory) {

    suspend fun insertMessage(message: Message): Message = databaseFactory.dbQuery {
        MessageTable.insert {
            it[productName] = message.productName
            it[content] = message.message
        }
        message
    }

    suspend fun findAll(): List<Message> = databaseFactory.dbQuery {
        MessageTable.selectAll().map {
            Message(
                productName = it[MessageTable.productName],
                message = it[MessageTable.content]
            )
        }
    }
}

