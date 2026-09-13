package com.message.api.message_api

import org.springframework.stereotype.Service

@Service
class MessageService(private val messageRepository: MessageRepository) {

    suspend fun saveMessage(message: Message): Message {
        return messageRepository.insertMessage(message)
    }

    suspend fun getAllMessages(): List<Message> {
        return messageRepository.findAll()
    }
}
