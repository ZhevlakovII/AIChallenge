package ru.izhxx.aichallenge.data.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.ParsedResponse
import ru.izhxx.aichallenge.domain.model.ResponseFormat

/**
 * Парсер для разбора XML и JSON ответов от LLM
 * Поддерживает извлечение структурированных данных из смешанных ответов
 */
object ResponseParser {
    private val logger = Logger.forClass(this)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Извлекает JSON из текста, если ответ содержит и JSON, и другой текст
     */
    fun extractJsonFromText(text: String): String? {
        logger.d("Попытка извлечь JSON из смешанного ответа")
        
        // Ищем первую открывающую и последнюю закрывающую скобку
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            val potentialJson = text.substring(startIndex, endIndex + 1)
            if (validateJson(potentialJson)) {
                logger.d("JSON успешно извлечен из смешанного ответа")
                return potentialJson
            }
        }
        
        // Более сложный подход с регулярным выражением
        val jsonPattern = """\{(?:[^{}]|(?:\{(?:[^{}]|(?:\{[^{}]*\}))*\}))*\}""".toRegex()
        val matches = jsonPattern.findAll(text)
        
        for (match in matches) {
            val potentialJson = match.value
            if (validateJson(potentialJson)) {
                logger.d("JSON успешно извлечен с помощью регулярного выражения")
                return potentialJson
            }
        }
        
        logger.w("Не удалось извлечь JSON из ответа")
        return null
    }
    
    /**
     * Извлекает XML из текста, если ответ содержит и XML, и другой текст
     */
    fun extractXmlFromText(text: String): String? {
        logger.d("Попытка извлечь XML из смешанного ответа")
        
        // Ищем тег <response>
        val startIndex = text.indexOf("<response>")
        val endIndex = text.lastIndexOf("</response>")
        
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            val potentialXml = text.substring(startIndex, endIndex + 11) // 11 = длина </response>
            if (validateXml(potentialXml)) {
                logger.d("XML успешно извлечен из смешанного ответа")
                return potentialXml
            }
        }
        
        // Более сложный подход с регулярным выражением
        val xmlPattern = """<response>.*?</response>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = xmlPattern.find(text)
        
        match?.value?.let {
            if (validateXml(it)) {
                logger.d("XML успешно извлечен с помощью регулярного выражения")
                return it
            }
        }
        
        logger.w("Не удалось извлечь XML из ответа")
        return null
    }
    
    /**
     * Парсит ответ в зависимости от формата
     * Предварительно пытается извлечь структурированные данные из смешанного текста
     */
    fun parseResponse(
        text: String,
        format: ResponseFormat
    ): Result<ParsedResponse> {
        logger.d("Начало парсинга ответа в формате $format")
        
        // Сначала пытаемся найти структурированные данные в тексте
        val extractedText = when (format) {
            ResponseFormat.JSON -> {
                // Если текст начинается с { и заканчивается на } - используем его как есть
                if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
                    logger.d("Ответ уже в формате JSON")
                    text
                } else {
                    logger.d("Ответ содержит смешанный контент, извлекаем JSON")
                    extractJsonFromText(text) ?: text
                }
            }
            ResponseFormat.XML -> {
                // Если текст начинается с <response> и заканчивается на </response> - используем его как есть
                if (text.trim().startsWith("<response>") && text.trim().endsWith("</response>")) {
                    logger.d("Ответ уже в формате XML")
                    text
                } else {
                    logger.d("Ответ содержит смешанный контент, извлекаем XML")
                    extractXmlFromText(text) ?: text
                }
            }
            ResponseFormat.UNFORMATTED -> {
                logger.d("Ответ в формате без форматирования")
                text
            }
        }
        
        return try {
            when (format) {
                ResponseFormat.XML -> parseXmlResponse(extractedText)
                ResponseFormat.JSON -> parseJsonResponse(extractedText)
                ResponseFormat.UNFORMATTED -> parseUnformattedResponse(extractedText)
            }
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге ответа формата $format", e)
            Result.failure(e)
        }
    }

    /**
     * Парсит XML ответ
     */
    private fun parseXmlResponse(text: String): Result<ParsedResponse> {
        return try {
            // Валидируем XML
            if (!validateXml(text)) {
                logger.w("XML ответ не валиден")
                return Result.failure(
                    Exception("Ответ от LLM не соответствует XML формату")
                )
            }

            // Извлекаем данные из XML
            val summary = extractXmlValue(text, "summary") ?: ""
            val explanation = extractXmlValue(text, "explanation") ?: ""
            val code = extractXmlValue(text, "code")
            val referencesText = extractXmlValue(text, "references")
            val references = parseReferences(referencesText)

            val parsedResponse = ParsedResponse(
                summary = summary,
                explanation = explanation,
                code = code,
                references = references,
                originalText = text,
                format = ResponseFormat.XML,
                isValid = true
            )

            Result.success(parsedResponse)
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге XML", e)
            Result.failure(e)
        }
    }

    /**
     * Парсит ответ без форматирования
     * Весь текст будет помещен в поле message, остальные поля будут пустыми
     */
    private fun parseUnformattedResponse(text: String): Result<ParsedResponse> {
        return try {
            val parsedResponse = ParsedResponse(
                summary = "",
                explanation = "",
                code = null,
                references = emptyList(),
                originalText = text,
                format = ResponseFormat.UNFORMATTED,
                isValid = true,
            )
            
            Result.success(parsedResponse)
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге неформатированного текста", e)
            Result.failure(e)
        }
    }
    
    /**
     * Парсит JSON ответ
     */
    private fun parseJsonResponse(text: String): Result<ParsedResponse> {
        return try {
            // Валидируем JSON
            if (!validateJson(text)) {
                logger.w("JSON ответ не валиден")
                return Result.failure(
                    Exception("Ответ от LLM не соответствует JSON формату")
                )
            }

            val jsonObject = json.parseToJsonElement(text).jsonObject

            val summary = jsonObject["summary"]?.jsonPrimitive?.content ?: ""
            val explanation = jsonObject["explanation"]?.jsonPrimitive?.content ?: ""
            val code = jsonObject["code"]?.jsonPrimitive?.content
            val references = mutableListOf<String>()

            jsonObject["references"]?.let { element ->
                if (element is JsonArray) {
                    references.addAll(element.map { it.jsonPrimitive.content })
                }
            }

            val parsedResponse = ParsedResponse(
                summary = summary,
                explanation = explanation,
                code = code,
                references = references,
                originalText = text,
                format = ResponseFormat.JSON,
                isValid = true
            )

            Result.success(parsedResponse)
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге JSON", e)
            Result.failure(e)
        }
    }

    /**
     * Валидирует XML структуру
     */
    fun validateXml(text: String): Boolean {
        return try {
            val trimmed = text.trim()
            if (!trimmed.startsWith("<") || !trimmed.endsWith(">")) {
                return false
            }

            // Проверяем наличие корневого элемента <response>
            if (!trimmed.contains("<response>") && !trimmed.contains("<response ")) {
                return false
            }

            if (!trimmed.contains("</response>")) {
                return false
            }

            true
        } catch (e: Exception) {
            logger.d("XML валидация не прошла: ${e.message}")
            false
        }
    }

    /**
     * Валидирует JSON структуру
     */
    fun validateJson(text: String): Boolean {
        return try {
            val trimmed = text.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return false
            }

            val jsonObject = json.parseToJsonElement(trimmed).jsonObject

            // Проверяем наличие необходимых полей
            jsonObject.containsKey("summary") && jsonObject.containsKey("explanation")
        } catch (e: Exception) {
            logger.d("JSON валидация не прошла: ${e.message}")
            false
        }
    }

    /**
     * Извлекает значение из XML по названию тега
     */
    private fun extractXmlValue(xml: String, tagName: String): String? {
        return try {
            val startTag = "<$tagName>"
            val endTag = "</$tagName>"
            val startIndex = xml.indexOf(startTag)
            val endIndex = xml.indexOf(endTag)

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                val valueStart = startIndex + startTag.length
                xml.substring(valueStart, endIndex).trim()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.d("Ошибка при извлечении XML значения для тага $tagName")
            null
        }
    }

    /**
     * Парсит references из строки или списка
     */
    private fun parseReferences(referencesText: String?): List<String> {
        if (referencesText.isNullOrBlank()) {
            return emptyList()
        }

        return referencesText
            .split("[,;\\n]".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Получает отображаемый текст из парсённого ответа
     */
    fun getDisplayText(parsed: ParsedResponse): String {
        // Для неформатированного ответа возвращаем сообщение как есть
        if (parsed.format == ResponseFormat.UNFORMATTED) {
            return parsed.originalText
        }
        
        return buildString {
            if (parsed.summary.isNotBlank()) {
                appendLine("${parsed.summary}\n")
            }
            if (parsed.explanation.isNotBlank()) {
                appendLine(parsed.explanation)
            }
            if (!parsed.code.isNullOrBlank()) {
                appendLine("\n```\n${parsed.code}\n```")
            }
            if (parsed.references.isNotEmpty()) {
                appendLine("\nИсточники:")
                parsed.references.forEach { ref ->
                    appendLine("- $ref")
                }
            }
        }.trim()
    }
}
