@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import kotlinx.datetime.Instant
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.CommonIssue
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketAnalytics
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Implementation of AnalyzeTicketsDataUseCase
 */
class AnalyzeTicketsDataUseCaseImpl(
    private val repository: ProductAssistantRepository
) : AnalyzeTicketsDataUseCase {

    override suspend fun invoke(
        statusFilter: String?,
        tagFilter: String?
    ): Result<TicketAnalytics> {
        return runCatching {
            // Get all tickets with optional filters
            val tickets = repository.getAllTickets(statusFilter, tagFilter).getOrThrow()

            if (tickets.isEmpty()) {
                return@runCatching TicketAnalytics(
                    totalTickets = 0,
                    statusDistribution = emptyMap(),
                    tagDistribution = emptyMap(),
                    mostCommonIssues = emptyList(),
                    averageResolutionTime = null,
                    openTicketsCount = 0,
                    resolvedTicketsCount = 0,
                    closedTicketsCount = 0,
                    inProgressTicketsCount = 0
                )
            }

            // Status distribution
            val statusDistribution = tickets.groupBy { it.status }
                .mapValues { it.value.size }

            // Tag distribution
            val tagDistribution = tickets
                .flatMap { ticket -> ticket.tags }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            // Most common issues (based on tags)
            val mostCommonIssues = tagDistribution
                .entries
                .take(10)
                .map { (tag, count) ->
                    val relatedTickets = tickets.filter { tag in it.tags }
                    val percentage = (count.toDouble() / tickets.size) * 100

                    // Generate description based on the tag
                    val description = when {
                        relatedTickets.isEmpty() -> "Нет доступных данных"
                        else -> {
                            val openCount = relatedTickets.count { it.status == TicketStatus.OPEN }
                            val resolvedCount = relatedTickets.count { it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED }
                            "Открыто: $openCount, Решено: $resolvedCount. " +
                                    relatedTickets.firstOrNull()?.title?.take(50)?.let { "Пример: \"$it...\"" }.orEmpty()
                        }
                    }

                    CommonIssue(
                        tag = tag,
                        count = count,
                        percentage = percentage,
                        relatedTickets = relatedTickets.map { it.id },
                        description = description
                    )
                }

            // Calculate average resolution time for resolved/closed tickets
            val resolvedTickets = tickets.filter {
                it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED
            }

            val averageResolutionTime = if (resolvedTickets.isNotEmpty()) {
                val totalHours = resolvedTickets.sumOf { ticket ->
                    calculateResolutionTimeHours(ticket.createdAt, ticket.updatedAt)
                }
                totalHours / resolvedTickets.size
            } else {
                null
            }

            // Counts by status
            val openCount = tickets.count { it.status == TicketStatus.OPEN }
            val resolvedCount = tickets.count { it.status == TicketStatus.RESOLVED }
            val closedCount = tickets.count { it.status == TicketStatus.CLOSED }
            val inProgressCount = tickets.count { it.status == TicketStatus.IN_PROGRESS }

            TicketAnalytics(
                totalTickets = tickets.size,
                statusDistribution = statusDistribution,
                tagDistribution = tagDistribution,
                mostCommonIssues = mostCommonIssues,
                averageResolutionTime = averageResolutionTime,
                openTicketsCount = openCount,
                resolvedTicketsCount = resolvedCount,
                closedTicketsCount = closedCount,
                inProgressTicketsCount = inProgressCount
            )
        }
    }

    private fun calculateResolutionTimeHours(createdAt: Instant, updatedAt: Instant): Double {
        val duration = updatedAt - createdAt
        return duration.inWholeHours.toDouble()
    }
}
