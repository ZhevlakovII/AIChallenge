package ru.izhxx.aichallenge.domain.model

/**
 * Содержит системные промпты для указания формата ответа LLM
 */
object FormatSystemPrompts {
    /**
     * Системный промпт для JSON формата ответа
     */
    val JSON_FORMAT_PROMPT = """
    Отвечай ТОЛЬКО валидным JSON объектом. Ничего кроме JSON. Без markdown-форматирования, пояснений до или после JSON.
    Строго соблюдай структуру:
    {
      "summary": "краткая суть (1-2 предложения)",
      "explanation": "подробное объяснение",
      "code": "код если требуется, или null",
      "references": ["источник1", "источник2"]
    }
    Твой ответ должен начинаться с '{' и заканчиваться '}'.
    """.trimIndent()
    /**
     * Системный промпт для XML формата ответа
     */
    val XML_FORMAT_PROMPT = """
    Отвечай ТОЛЬКО валидным XML. Ничего кроме XML. Без markdown-форматирования, пояснений до или после XML.
    Строго соблюдай структуру:
    <response>
      <summary>краткая суть (1-2 предложения)</summary>
      <explanation>подробное объяснение</explanation>
      <code>код если требуется</code>
      <references>источник1, источник2</references>
    </response>
    Твой ответ должен начинаться с '<response>' и заканчиваться '</response>'.
    """.trimIndent()
    
    /**
     * Системный промпт для формата без форматирования
     * Позволяет получить прямой ответ от модели без структурирования
     */
    val UNFORMATTED_PROMPT = """
    Отвечай обычным текстом без специального форматирования. 
    Просто дай прямой ответ на вопрос пользователя.
    """.trimIndent()
    
    /**
     * Получить промпт для указанного формата ответа
     */
    fun getFormatPrompt(format: ResponseFormat): String {
        return when(format) {
            ResponseFormat.JSON -> JSON_FORMAT_PROMPT
            ResponseFormat.XML -> XML_FORMAT_PROMPT
            ResponseFormat.UNFORMATTED -> UNFORMATTED_PROMPT
        }
    }
}
