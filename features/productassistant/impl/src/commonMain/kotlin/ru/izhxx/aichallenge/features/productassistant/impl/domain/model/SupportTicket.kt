package ru.izhxx.aichallenge.features.productassistant.impl.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Domain model for a support ticket
 */
@ExperimentalTime
data class SupportTicket(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val status: TicketStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tags: List<String>,
    val comments: List<TicketComment> = emptyList()
)

/**
 * Domain model for a ticket comment
 */
@ExperimentalTime
data class TicketComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
    val isInternal: Boolean = false
)

/**
 * Status of a support ticket
 */
enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    companion object {
        fun fromString(value: String): TicketStatus {
            return when (value.lowercase()) {
                "open" -> OPEN
                "in_progress" -> IN_PROGRESS
                "resolved" -> RESOLVED
                "closed" -> CLOSED
                else -> OPEN
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            OPEN -> "Открыт"
            IN_PROGRESS -> "В работе"
            RESOLVED -> "Решён"
            CLOSED -> "Закрыт"
        }
    }
}
