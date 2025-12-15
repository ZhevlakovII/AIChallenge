package ru.izhxx.aichallenge.features.productassistant.impl.domain.model

/**
 * User query to the Product Assistant
 */
data class AssistantQuery(
    val text: String,
    val mode: AssistantMode = AssistantMode.FULL,
    val ticketId: String? = null
)

/**
 * Mode of operation for the Product Assistant
 */
enum class AssistantMode {
    /**
     * Mode A: Questions about the product (FAQ + RAG)
     */
    FAQ_ONLY,

    /**
     * Mode B: Ticket analysis (MCP)
     */
    TICKET_ANALYSIS,

    /**
     * Mode C: Full mode (FAQ + MCP combined)
     */
    FULL,

    /**
     * Mode D: Data Analytics mode - analyze ticket data, statistics, trends
     */
    ANALYTICS;

    fun toDisplayString(): String {
        return when (this) {
            FAQ_ONLY -> "Режим FAQ"
            TICKET_ANALYSIS -> "Анализ тикетов"
            FULL -> "Полный режим"
            ANALYTICS -> "Аналитика данных"
        }
    }
}
