package com.message.api.message_api.controller

import com.message.api.message_api.Message
import com.message.api.message_api.MessageService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageController(private val messageService: MessageService) {

    @PostMapping("/message")
    suspend fun createMessage(@RequestBody message: Message): Message {
        return messageService.saveMessage(message)
    }

    @GetMapping("/message")
    suspend fun getAllMessages(): List<Message> {
        return messageService.getAllMessages()
    }

}

