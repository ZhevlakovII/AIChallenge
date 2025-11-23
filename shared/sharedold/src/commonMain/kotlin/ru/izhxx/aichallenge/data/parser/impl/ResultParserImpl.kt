package ru.izhxx.aichallenge.data.parser.impl

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.parser.core.JsonParser
import ru.izhxx.aichallenge.data.parser.core.MarkdownParser
import ru.izhxx.aichallenge.data.parser.core.ResultParser
import ru.izhxx.aichallenge.domain.model.config.ResponseFormat
import ru.izhxx.aichallenge.domain.model.message.ParsedMessage

/**
 * Основная реализация парсера результатов
 * Является единой точкой входа для парсинга ответов от LLM
 * Маршрутизирует запрос к соответствующему парсеру в зависимости от формата
 */
class ResultParserImpl(
    private val jsonParser: JsonParser,
    private val markdownParser: MarkdownParser
) : ResultParser {
    private val logger = Logger.forClass(this::class)

    /**
     * Парсит ответ в соответствии с указанным форматом
     *
     * @param input пара (текст, формат ответа)
     * @return результат парсинга в виде ParsedMessage
     */
    override fun parse(input: Pair<String, ResponseFormat>): Result<ParsedMessage> {
        val (text, format) = input
        logger.d("Начало парсинга ответа в формате $format")

        return try {
            when (format) {
                // Для JSON формата используем JsonParser, который может извлекать JSON 
                // из смешанного текста и преобразовывать его в структурированное сообщение
                ResponseFormat.JSON -> {
                    jsonParser.parse(text)
                }

                // Для Markdown формата используем MarkdownParser для парсинга 
                // блочных элементов и преобразования их в сообщение с Markdown
                ResponseFormat.MARKDOWN -> {
                    markdownParser.parse(text).map { nodes ->
                        ParsedMessage.Markdown(nodes)
                    }
                }

                // Для простого текста без форматирования просто оборачиваем его 
                // в ParsedMessage.Plain без дополнительной обработки
                ResponseFormat.PLAIN -> {
                    Result.success(ParsedMessage.Plain(text))
                }
            }
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге ответа формата $format", e)
            Result.failure(e)
        }
    }
}
