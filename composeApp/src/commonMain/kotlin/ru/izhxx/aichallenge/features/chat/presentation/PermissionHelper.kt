package ru.izhxx.aichallenge.features.chat.presentation

import androidx.compose.runtime.Composable

/**
 * Запрашивает разрешение на запись аудио для распознавания речи
 *
 * @param hasPermission Текущее состояние разрешения
 * @param onPermissionResult Callback с результатом запроса разрешения
 */
@Composable
expect fun RequestAudioPermission(
    hasPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit
)
