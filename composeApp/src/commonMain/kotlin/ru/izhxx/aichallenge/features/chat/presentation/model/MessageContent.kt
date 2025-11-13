package ru.izhxx.aichallenge.features.chat.presentation.model

import ru.izhxx.aichallenge.domain.model.markdown.InlineElement
import ru.izhxx.aichallenge.domain.model.markdown.MarkdownNode

/**
 * Содержимое сообщения в различных форматах
 */
sealed interface MessageContent {
    /**
     * Простой текст без форматирования
     */
    data class Plain(val text: String) : MessageContent

    /**
     * Markdown-форматированное сообщение
     */
    data class Markdown(val nodes: List<MarkdownNode>) : MessageContent

    /**
     * Структурированное содержимое с разными секциями
     */
    data class Structured(
        val summary: MarkdownNode.Paragraph,
        val explanation: MarkdownNode.Paragraph,
        val code: MarkdownNode.CodeBlock? = null,
        val references: List<InlineElement.Link> = emptyList()
    ) : MessageContent
}
