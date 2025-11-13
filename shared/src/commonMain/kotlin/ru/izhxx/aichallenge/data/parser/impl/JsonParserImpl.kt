package ru.izhxx.aichallenge.data.parser.impl

import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.parser.LLMContentJsonModel
import ru.izhxx.aichallenge.data.parser.core.JsonParser
import ru.izhxx.aichallenge.data.parser.core.MarkdownParser
import ru.izhxx.aichallenge.domain.model.markdown.InlineElement
import ru.izhxx.aichallenge.domain.model.markdown.MarkdownNode
import ru.izhxx.aichallenge.domain.model.message.ParsedMessage

/**
 * Реализация парсера JSON
 * Отвечает за преобразование JSON в структурированное сообщение
 * Использует сериализацию для преобразования JSON в модель JsonLLMResponse
 */
class JsonParserImpl(
    private val markdownParser: MarkdownParser,
    private val json: Json
) : JsonParser {
    private val logger = Logger.forClass(this::class)

    /**
     * Парсит JSON и преобразует его в структурированное сообщение
     *
     * @param input строка с JSON
     * @return результат парсинга в виде ParsedMessage.Structured
     */
    override fun parse(input: String): Result<ParsedMessage.Structured> {
        return try {
            // Преобразуем JsonLLMResponse в ParsedMessage.Structured
            val parsedResponse = convertToParsedMessage(
                json.decodeFromString<LLMContentJsonModel>(input)
            )

            Result.success(parsedResponse)
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге JSON", e)
            Result.failure(e)
        }
    }

    /**
     * Преобразует JsonLLMResponse в ParsedMessage.Structured
     */
    private fun convertToParsedMessage(response: LLMContentJsonModel): ParsedMessage.Structured {
        return ParsedMessage.Structured(
            summary = MarkdownNode.Paragraph(listOf(InlineElement.Text(response.summary))),
            explanation = MarkdownNode.Paragraph(markdownParser.parseInlineElements(response.explanation)),
            code = response.code?.let { MarkdownNode.CodeBlock(it) },
            references = response.references.map { InlineElement.Link(it, it) }
        )
    }
}
