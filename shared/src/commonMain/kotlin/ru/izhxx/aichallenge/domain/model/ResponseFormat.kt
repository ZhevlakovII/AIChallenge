package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Формат ответа LLM
 */
@Serializable
enum class ResponseFormat {
    XML,
    JSON,
    UNFORMATTED;
}
