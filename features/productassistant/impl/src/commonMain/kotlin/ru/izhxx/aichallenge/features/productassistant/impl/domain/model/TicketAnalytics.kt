package ru.izhxx.aichallenge.features.productassistant.impl.domain.model

/**
 * Aggregated analytics data from support tickets
 */
data class TicketAnalytics(
    val totalTickets: Int,
    val statusDistribution: Map<TicketStatus, Int>,
    val tagDistribution: Map<String, Int>,
    val mostCommonIssues: List<CommonIssue>,
    val averageResolutionTime: Double?, // In hours
    val openTicketsCount: Int,
    val resolvedTicketsCount: Int,
    val closedTicketsCount: Int,
    val inProgressTicketsCount: Int
)

/**
 * Represents a common issue found in tickets
 */
data class CommonIssue(
    val tag: String,
    val count: Int,
    val percentage: Double,
    val relatedTickets: List<String>, // Ticket IDs
    val description: String
)

/**
 * Time-based ticket statistics
 */
data class TimeBasedStatistics(
    val ticketsPerDay: Map<String, Int>, // Date -> Count
    val peakHours: List<Int>, // Hours of day with most tickets
    val averageTicketsPerWeek: Double
)

/**
 * Tag-based analysis
 */
data class TagAnalysis(
    val tag: String,
    val totalCount: Int,
    val openCount: Int,
    val resolvedCount: Int,
    val averageResolutionTime: Double?, // In hours
    val trend: Trend
)

enum class Trend {
    INCREASING,
    DECREASING,
    STABLE
}
