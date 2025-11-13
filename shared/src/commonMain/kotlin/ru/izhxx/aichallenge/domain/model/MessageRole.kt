package ru.izhxx.aichallenge.domain.model

/**
 * Роль сообщения в контексте общения с LLM
 * @property key Ключ роли для API (в lowercase)
 */
enum class MessageRole(val key: String) {
    /**
     * Системное сообщение, определяющее контекст и инструкции для модели
     */
    SYSTEM("system"),

    /**
     * Сообщение пользователя
     */
    USER("user"),

    /**
     * Сообщение от ассистента (LLM)
     */
    ASSISTANT("assistant"),

    /**
     * Техническое сообщение (информационные сообщения, ошибки и т.д.)
     */
    TECH("tech");

    companion object {
        @JvmStatic
        fun getRole(roleString: String?): MessageRole =
            entries.first { it.key == roleString?.lowercase() }
    }
}