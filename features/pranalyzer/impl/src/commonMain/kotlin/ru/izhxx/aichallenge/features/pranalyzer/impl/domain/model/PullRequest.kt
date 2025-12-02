package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

/**
 * Represents basic information about a Pull Request
 */
@OptIn(ExperimentalTime::class)
data class PullRequest(
    val number: Int,
    val title: String,
    val description: String,
    val author: String,
    val baseBranch: String,
    val headBranch: String,
    val state: PrState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val url: String,
    val filesChanged: Int,
    val additions: Int,
    val deletions: Int,
    val commits: Int
)

/**
 * State of a Pull Request
 */
enum class PrState {
    OPEN,
    CLOSED,
    MERGED,
    DRAFT
}
