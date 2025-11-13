package ru.izhxx.aichallenge.domain.model.markdown

/**
 * Интерфейс для блочных элементов Markdown
 * Представляет структурные элементы документа
 */
sealed interface MarkdownNode {
    /**
     * Параграф с инлайн-элементами
     */
    data class Paragraph(val elements: List<InlineElement>) : MarkdownNode
    
    /**
     * Заголовок
     */
    data class Heading(val level: Int, val elements: List<InlineElement>) : MarkdownNode
    
    /**
     * Блок кода
     */
    data class CodeBlock(val code: String, val language: String = "") : MarkdownNode
    
    /**
     * Ненумерованный список
     */
    data class UnorderedList(val items: List<ListItem>) : MarkdownNode
    
    /**
     * Нумерованный список
     */
    data class OrderedList(val items: List<ListItem>) : MarkdownNode
    
    /**
     * Элемент списка
     */
    data class ListItem(val elements: List<InlineElement>) : MarkdownNode
    
    /**
     * Цитата
     */
    data class Quote(val elements: List<InlineElement>) : MarkdownNode
    
    /**
     * Горизонтальная линия
     */
    object HorizontalRule : MarkdownNode
    
    /**
     * Разделитель (пустая строка)
     */
    object Divider : MarkdownNode
}
