package com.example.data

data class ActionResult(
    val success: Boolean,
    val actionType: String,
    val message: String,
    val details: String? = null
)

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val lookupKey: String? = null
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val language: String? = null,
    val actionResult: ActionResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ARUSHI,
    SYSTEM
}

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING_ACTION,
    SPEAKING
}
