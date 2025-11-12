package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель для представления элементов markdown
 */
@Serializable
sealed class MarkdownElement {
    @Serializable
    data class Text(val content: String) : MarkdownElement()
    @Serializable
    data class Bold(val content: String) : MarkdownElement()
    @Serializable
    data class Italic(val content: String) : MarkdownElement()
    @Serializable
    data class InlineCode(val content: String) : MarkdownElement()
    @Serializable
    data class Link(val text: String, val url: String) : MarkdownElement()
    @Serializable
    data class CodeBlock(val code: String, val language: String = "") : MarkdownElement()
    @Serializable
    data class Heading(val level: Int, val content: String) : MarkdownElement()
    @Serializable
    data class UnorderedList(val items: List<String>) : MarkdownElement()
    @Serializable
    data class OrderedList(val items: List<String>) : MarkdownElement()
    @Serializable
    data class Quote(val content: String) : MarkdownElement()
    @Serializable
    data class Paragraph(val items: List<MarkdownElement>) : MarkdownElement()
}
