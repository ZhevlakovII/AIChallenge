package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Метрики запроса к LLM API
 * Содержит информацию о времени выполнения и использованных токенах
 */
@Serializable
data class RequestMetrics(
    val responseTime: Long,      // время ответа в миллисекундах
    val tokensInput: Int,        // входные токены (prompt tokens)
    val tokensOutput: Int,       // выходные токены (completion tokens)
    val tokensTotal: Int         // всего токенов
)
