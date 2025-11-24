package ru.izhxx.aichallenge.domain.model.markdown

import kotlinx.serialization.Serializable

/**
 * Интерфейс для инлайн-элементов Markdown
 * Представляет элементы форматирования текста внутри блоков
 */
sealed interface InlineElement {
    /**
     * Простой текст без форматирования
     */
    data class Text(val text: String) : InlineElement
    
    /**
     * Жирный текст
     */
    data class Bold(val elements: List<InlineElement>) : InlineElement
    
    /**
     * Курсивный текст
     */
    data class Italic(val elements: List<InlineElement>) : InlineElement
    
    /**
     * Инлайн-код
     */
    data class Code(val code: String) : InlineElement
    
    /**
     * Ссылка
     */
    data class Link(val text: String, val url: String) : InlineElement
    
    /**
     * Изображение
     */
    data class Image(val altText: String, val url: String) : InlineElement
    
    /**
     * Перечёркнутый текст
     */
    data class Strikethrough(val elements: List<InlineElement>) : InlineElement
}
