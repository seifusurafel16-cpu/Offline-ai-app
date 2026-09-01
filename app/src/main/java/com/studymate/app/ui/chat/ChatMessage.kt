package com.studymate.app.ui.chat

/**
 * A single chat message rendered in the Chat tab.
 * [id] is a stable identifier for Compose lazy lists.
 */
data class ChatMessage(
    val id: Long,
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
) {
    enum class Role { USER, ASSISTANT }

    companion object {
        private var counter = 0L
        fun nextId(): Long = synchronized(Companion) { counter++ }
    }
}
