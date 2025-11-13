package ru.izhxx.aichallenge.features.chat.presentation.model

/**
 * Метаданные сообщения (время ответа, статистика токенов и т.д.)
 */
data class MessageMetadata(
    val responseTimeMs: Long,
    val tokensInput: Int,
    val tokensOutput: Int,
    val tokensTotal: Int,
    val responseFormat: String
)
