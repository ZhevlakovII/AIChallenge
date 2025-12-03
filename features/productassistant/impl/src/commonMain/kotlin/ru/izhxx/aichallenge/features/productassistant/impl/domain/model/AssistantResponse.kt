package ru.izhxx.aichallenge.features.productassistant.impl.domain.model

import kotlin.time.ExperimentalTime

/**
 * Response from the Product Assistant
 */
@OptIn(ExperimentalTime::class)
data class AssistantResponse(
    val answer: String,
    val mode: AssistantMode,
    val relatedTickets: List<SupportTicket> = emptyList(),
    val relatedDocumentation: List<DocumentationItem> = emptyList(),
    val confidence: Double = 0.0,
    val sources: List<ResponseSource> = emptyList()
)

/**
 * Source of information for the assistant's response
 */
data class ResponseSource(
    val type: SourceType,
    val reference: String,
    val excerpt: String? = null
)

/**
 * Type of information source
 */
enum class SourceType {
    FAQ,
    TICKET,
    DOCUMENTATION,
    LLM_KNOWLEDGE;

    fun toDisplayString(): String {
        return when (this) {
            FAQ -> "FAQ"
            TICKET -> "Тикет поддержки"
            DOCUMENTATION -> "Документация"
            LLM_KNOWLEDGE -> "Знания LLM"
        }
    }
}
