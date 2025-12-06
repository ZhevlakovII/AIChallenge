package ru.izhxx.aichallenge.instruments.llm.interactions.api.model

/**
 * Роли сообщений при работе с LLM.
 */
enum class MessageRole(val key: String) {
    System("system"),
    Assistant("assistant"),
    User("user")
}