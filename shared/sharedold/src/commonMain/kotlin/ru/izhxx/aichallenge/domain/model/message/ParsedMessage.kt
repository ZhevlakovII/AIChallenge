package ru.izhxx.aichallenge.domain.model.message

import ru.izhxx.aichallenge.domain.model.markdown.InlineElement
import ru.izhxx.aichallenge.domain.model.markdown.MarkdownNode

/**
 * Интерфейс для разобранного сообщения
 * Разные типы контента могут иметь разную структуру
 */
sealed interface ParsedMessage {
    /**
     * Структурированное содержимое, полученное из JSON ответа LLM
     */
    data class Structured(
        val summary: MarkdownNode.Paragraph,
        val explanation: MarkdownNode.Paragraph,
        val code: MarkdownNode.CodeBlock? = null,
        val references: List<InlineElement.Link> = emptyList()
    ) : ParsedMessage
    
    /**
     * Markdown-форматированное сообщение
     */
    data class Markdown(val nodes: List<MarkdownNode>) : ParsedMessage
    
    /**
     * Обычный текст без разметки
     */
    data class Plain(val text: String) : ParsedMessage
}
