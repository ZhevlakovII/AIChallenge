package ru.izhxx.aichallenge.tools.llm.completions.api.model

/**
 * Информация об использовании токенов в запросе к LLM.
 * Предоставляется провайдером API для отслеживания расхода ресурсов.
 *
 * @property promtTokens Количество токенов во входящих сообщениях (промпт, история).
 * @property completionTokens Количество токенов в сгенерированном ответе.
 * @property totalTokens Общее количество использованных токенов (promtTokens + completionTokens).
 */
class Usage(
    val promtTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
