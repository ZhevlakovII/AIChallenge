package ru.izhxx.aichallenge.features.chat.presentation

import androidx.compose.runtime.Composable

/**
 * JVM/Desktop заглушка - разрешения не требуются
 */
@Composable
actual fun RequestAudioPermission(
    hasPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit
) {
    // На Desktop нет системы разрешений, поэтому ничего не делаем
}
