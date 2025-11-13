package ru.izhxx.aichallenge.data.parser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель для JSON ответа от LLM
 * Используется для сериализации/десериализации JSON ответов
 */
@Serializable
class LLMContentJsonModel(
    /**
     * Краткое содержание (1-2 предложения)
     */
    @SerialName("summary")
    val summary: String,

    /**
     * Подробное объяснение с поддержкой инлайн markdown-элементов
     */
    @SerialName("explanation")
    val explanation: String,

    /**
     * Код, если требуется, или null
     */
    @SerialName("code")
    val code: String?,

    /**
     * Список ссылок на источники
     */
    @SerialName("references")
    val references: List<String>
)