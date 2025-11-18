package ru.izhxx.aichallenge.domain.model.config

/**
 * Настройки взаимодействия с LLM
 * Содержит параметры, которые влияют на промпт и генерацию ответов
 */
data class LLMConfig(
    /**
     * Температура генерации (влияет на случайность ответов)
     * Диапазон значений: 0.0 - 1.0
     * Более низкие значения делают ответы более детерминированными
     * Более высокие значения делают ответы более случайными и творческими
     */
    val temperature: Double,

    /**
     * Максимальное количество токенов для ответа
     * Ограничивает длину генерируемого текста
     */
    val maxTokens: Int,

    /**
     * Формат ответа LLM
     */
    val responseFormat: ResponseFormat,

    /**
     * Системный промпт, определяющий роль и поведение LLM
     */
    val systemPrompt: String,
    
    /**
     * Top-K параметр для выбора наиболее вероятных токенов
     * Определяет, сколько самых вероятных токенов будет рассмотрено при генерации
     */
    val topK: Int,
    
    /**
     * Top-P (ядро) параметр для выбора токенов
     * Определяет, какая часть общей вероятности должна быть охвачена выбранными токенами
     */
    val topP: Double,
    
    /**
     * Min-P параметр - минимальный порог вероятности для токена
     * Токены с вероятностью ниже этого значения не учитываются
     */
    val minP: Double,
    
    /**
     * Top-A параметр для выбора токенов
     * Альтернативный метод выбора, основанный на абсолютном преимуществе токена
     */
    val topA: Double,
    
    /**
     * Seed для генератора случайных чисел
     * Обеспечивает воспроизводимость результатов
     */
    val seed: Long,
    /**
     * Фича-флаг: включить поддержку function calling и MCP-инструментов.
     */
    val enableMcpToolCalling: Boolean = LLMDefaults.DEFAULT_ENABLE_MCP_TOOL_CALLING
) {
    companion object {
        /**
         * Создает настройки LLM по умолчанию
         */
        fun default() = LLMConfig(
            temperature = LLMDefaults.DEFAULT_TEMPERATURE,
            maxTokens = LLMDefaults.DEFAULT_MAX_TOKENS,
            responseFormat = ResponseFormat.MARKDOWN,
            systemPrompt = LLMDefaults.DEFAULT_SYSTEM_PROMPT,
            topK = LLMDefaults.DEFAULT_TOP_K,
            topP = LLMDefaults.DEFAULT_TOP_P,
            minP = LLMDefaults.DEFAULT_MIN_P,
            topA = LLMDefaults.DEFAULT_TOP_A,
            seed = LLMDefaults.DEFAULT_SEED,
            enableMcpToolCalling = LLMDefaults.DEFAULT_ENABLE_MCP_TOOL_CALLING
        )
    }
}
