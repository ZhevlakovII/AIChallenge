package ru.izhxx.aichallenge.domain.model

/**
 * Модель для метрик чата
 * Используется для отслеживания использования токенов и эффективности сжатия истории
 */
data class ChatMetrics(
    /**
     * Общее количество токенов запросов
     */
    val totalPromptTokens: Int = 0,

    /**
     * Общее количество токенов ответов
     */
    val totalCompletionTokens: Int = 0,

    /**
     * Общее количество токенов (запросы + ответы)
     */
    val totalTokens: Int = 0,

    /**
     * Количество сообщений в истории
     */
    val messageCount: Int = 0,

    /**
     * Количество выполненных сжатий истории
     */
    val compressionCount: Int = 0,

    /**
     * Среднее время ответа (мс)
     */
    val averageResponseTime: Long = 0,

    /**
     * Количество ответов
     */
    val responseCount: Int = 0
)