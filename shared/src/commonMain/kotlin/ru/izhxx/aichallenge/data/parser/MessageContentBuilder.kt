package ru.izhxx.aichallenge.data.parser

import ru.izhxx.aichallenge.domain.model.MarkdownElement
import ru.izhxx.aichallenge.domain.model.MessageContent
import ru.izhxx.aichallenge.domain.model.ParsedResponse
import ru.izhxx.aichallenge.domain.model.ResponseFormat

/**
 * Построитель содержимого сообщения из парсённого ответа
 * Преобразует структурированные (JSON/XML) и неструктурированные ответы в соответствующий MessageContent
 */
object MessageContentBuilder {

    /**
     * Строит MessageContent из парсённого ответа LLM
     *
     * Логика:
     * 1. Если ответ имеет структуру (JSON/XML) и валиден - создаёт Structured content
     * 2. Если ответ неформатированный - используется Markdown парсер
     * 3. Fallback на Plain текст если всё сломалось
     */
    fun buildMessageContent(
        parsedResponse: ParsedResponse,
        format: ResponseFormat
    ): MessageContent {
        return when {
            format != ResponseFormat.UNFORMATTED && parsedResponse.isValid -> {
                try {
                    // Есть структура (JSON/XML) - создаём Structured
                    MessageContent.Structured(
                        summary = parseTextToMarkdownParagraph(parsedResponse.summary),
                        explanation = parseTextToMarkdownParagraph(parsedResponse.explanation),
                        code = if (parsedResponse.code != null) {
                            MarkdownElement.CodeBlock(parsedResponse.code)
                        } else {
                            null
                        },
                        references = parsedResponse.references.map { ref ->
                            MarkdownElement.Link(text = ref, url = "")
                        }
                    )
                } catch (e: Exception) {
                    // Если парсинг структурированного контента упал, используем markdown
                    MessageContent.Markdown(
                        elements = MarkdownParser.parse(parsedResponse.originalText)
                    )
                }
            }

            parsedResponse.format == ResponseFormat.UNFORMATTED -> {
                try {
                    // Нет структуры - используем markdown парсер
                    MessageContent.Markdown(
                        elements = MarkdownParser.parse(parsedResponse.originalText)
                    )
                } catch (e: Exception) {
                    // Fallback на plain text
                    MessageContent.Plain(parsedResponse.originalText)
                }
            }

            else -> {
                // Fallback на plain text
                MessageContent.Plain(parsedResponse.originalText)
            }
        }
    }

    /**
     * Преобразует обычный текст в Paragraph с парсингом inline элементов
     */
    private fun parseTextToMarkdownParagraph(text: String): MarkdownElement.Paragraph {
        if (text.isBlank()) {
            return MarkdownElement.Paragraph(listOf(MarkdownElement.Text("")))
        }

        // Парсим текст через markdown парсер
        val elements = MarkdownParser.parse(text)

        // Извлекаем inline элементы из распарсенного контента
        val inlineElements = mutableListOf<MarkdownElement>()

        for (element in elements) {
            when (element) {
                is MarkdownElement.Paragraph -> {
                    // Если это уже paragraph, берём его элементы
                    inlineElements.addAll(element.items)
                }
                else -> {
                    // Другие элементы добавляем как есть
                    inlineElements.add(element)
                }
            }
        }

        return if (inlineElements.isNotEmpty()) {
            MarkdownElement.Paragraph(inlineElements)
        } else {
            MarkdownElement.Paragraph(listOf(MarkdownElement.Text(text)))
        }
    }
}
