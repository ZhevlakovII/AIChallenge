package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

/**
 * Represents a file that was modified in a Pull Request
 */
data class PrFile(
    val filename: String,
    val status: FileStatus,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val patch: String?,
    val blobUrl: String,
    val rawUrl: String
)

/**
 * Status of a file in a Pull Request
 */
enum class FileStatus {
    ADDED,
    MODIFIED,
    REMOVED,
    RENAMED,
    COPIED,
    CHANGED,
    UNCHANGED
}
