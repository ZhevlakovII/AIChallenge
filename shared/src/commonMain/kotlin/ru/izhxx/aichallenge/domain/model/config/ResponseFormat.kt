package ru.izhxx.aichallenge.domain.model.config

/**
 * Формат ответа от LLM
 */
enum class ResponseFormat {
    /**
     * Ответ в формате JSON
     * Используется для структурированных данных, которые могут быть легко преобразованы в объекты
     */
    JSON,

    /**
     * Ответ в формате Markdown
     * Позволяет форматировать текст с заголовками, списками, выделением, кодовыми блоками и т.д.
     */
    MARKDOWN,

    /**
     * Ответ в виде обычного текста без форматирования
     * Используется, когда не требуется специальное форматирование или структура
     */
    PLAIN;

    companion object {
        @JvmStatic
        fun getFormat(formatString: String?): ResponseFormat =
            entries.firstOrNull { it.name.lowercase() == formatString?.lowercase() } ?: PLAIN
    }
}
