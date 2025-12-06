package ru.izhxx.aichallenge.instruments.llm.interactions.api.model

/**
 * Роли сообщений при работе с LLM.
 */
enum class MessageRole(val key: String) {
    System("system"),
    Assistant("assistant"),
    User("user");

    companion object {
        fun parseRole(roleString: String): MessageRole {
            return entries.first { role -> roleString.lowercase() == role.key.lowercase() }
        }
    }
}