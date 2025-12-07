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
    val tags: List<String>
)

/**
 * Status of a support ticket
 */
enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED;

    companion object {
        fun fromString(value: String): TicketStatus {
            return when (value.lowercase()) {
                "open" -> OPEN
                "in_progress" -> IN_PROGRESS
                "resolved" -> RESOLVED
                else -> OPEN
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            OPEN -> "Открыт"
            IN_PROGRESS -> "В работе"
            RESOLVED -> "Решён"
        }
    }
}
