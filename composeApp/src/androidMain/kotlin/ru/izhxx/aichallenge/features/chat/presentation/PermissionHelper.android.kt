package ru.izhxx.aichallenge.features.chat.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Android-реализация запроса разрешения на запись аудио
 */
@Composable
actual fun RequestAudioPermission(
    hasPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit
) {
    // Флаг для отслеживания, был ли уже запрошен permission
    var permissionRequested by remember { mutableStateOf(false) }

    // Создаем launcher для запроса разрешения
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            onPermissionResult(isGranted)
            permissionRequested = true
        }
    )

    // Автоматически запрашиваем разрешение при первом запуске, если его нет и оно еще не запрашивалось
    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
