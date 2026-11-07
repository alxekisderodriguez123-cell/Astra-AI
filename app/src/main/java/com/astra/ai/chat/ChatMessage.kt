
package com.astra.ai.chat

data class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long
)

data class ChatConversation(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
