package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

/**
 * Represents the complete diff information for a Pull Request
 */
data class PrDiff(
    val prNumber: Int,
    val files: List<PrFile>,
    val totalAdditions: Int,
    val totalDeletions: Int,
    val totalChanges: Int
)

/**
 * Represents a diff hunk within a file
 */
data class DiffHunk(
    val oldStart: Int,
    val oldLines: Int,
    val newStart: Int,
    val newLines: Int,
    val header: String,
    val content: String
)

/**
 * Represents detailed diff content for a single file
 */
data class FileDiff(
    val filename: String,
    val oldContent: String?,
    val newContent: String?,
    val hunks: List<DiffHunk>,
    val language: String?
)
