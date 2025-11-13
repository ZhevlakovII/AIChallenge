package ru.izhxx.aichallenge.data.parser.core

import ru.izhxx.aichallenge.domain.model.markdown.InlineElement
import ru.izhxx.aichallenge.domain.model.markdown.MarkdownNode

/**
 * Интерфейс для парсера Markdown
 * Отвечает за преобразование текста в формате Markdown в структуру MarkdownNode
 */
interface MarkdownParser : Parser<String, List<MarkdownNode>> {
    /**
     * Парсит инлайн-элементы Markdown (жирный, курсив, ссылки и т.д.)
     *
     * @param text текст для парсинга
     * @return список инлайн-элементов
     */
    fun parseInlineElements(text: String): List<InlineElement>
}
