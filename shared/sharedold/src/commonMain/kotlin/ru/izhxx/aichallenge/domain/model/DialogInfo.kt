package ru.izhxx.aichallenge.domain.model

/**
 * Информация о диалоге для отображения в списке
 */
data class DialogInfo(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)