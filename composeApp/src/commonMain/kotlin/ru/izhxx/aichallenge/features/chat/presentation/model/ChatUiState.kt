package ru.izhxx.aichallenge.features.chat.presentation.model

import ru.izhxx.aichallenge.domain.model.error.DomainException

/**
 * Состояние UI для фичи чата
 */
data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: DomainException? = null,
    val apiKeyConfigured: Boolean = false,
    // Voice recording state
    val isRecording: Boolean = false,
    val recognizedText: String = "",
    val hasRecordPermission: Boolean = false
)
