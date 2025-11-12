package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель для представления различных типов содержимого сообщения
 * Поддерживает структурированное (JSON/XML), markdown и простой текст
 */
@Serializable
sealed class MessageContent {
    /**
     * Структурированное содержимое, полученное из JSON/XML ответа LLM
     */
    @Serializable
    data class Structured(
        val summary: MarkdownElement.Paragraph,
        val explanation: MarkdownElement.Paragraph,
        val code: MarkdownElement.CodeBlock? = null,
        val references: List<MarkdownElement.Link> = emptyList()
    ) : MessageContent()

    /**
     * Markdown содержимое, используется когда нет структуры
     */
    @Serializable
    data class Markdown(val elements: List<MarkdownElement>) : MessageContent()

    /**
     * Простой текст без форматирования
     */
    @Serializable
    data class Plain(val text: String) : MessageContent()
}
