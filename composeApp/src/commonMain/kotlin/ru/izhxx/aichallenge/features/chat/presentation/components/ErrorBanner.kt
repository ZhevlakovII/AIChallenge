package ru.izhxx.aichallenge.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.domain.model.error.DomainException

/**
 * Компонент для отображения баннера с ошибкой
 */
@Composable
fun ErrorBanner(
    error: DomainException,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedMessage by remember { mutableStateOf(false) }
    
    // Определяем, какое сообщение показывать: короткое (если есть) или полное
    val displayText = error.shortMessage ?: error.detailedMessage
    val hasMoreDetails = error.shortMessage != null
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = hasMoreDetails,
                onClick = onShowDetails
            )
            .padding(12.dp)
    ) {
        Column {
            // Описание ошибки и кнопка копирования
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = { 
                        clipboardManager.setText(AnnotatedString(error.detailedMessage))
                        showCopiedMessage = true
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Копировать текст ошибки",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Сообщение о копировании
            if (showCopiedMessage) {
                Text(
                    text = "Скопировано в буфер обмена",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Ссылка "Показать подробности" (только если есть короткая версия сообщения)
            if (hasMoreDetails) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Показать подробности",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .clickable(onClick = onShowDetails)
                    )
                }
            }
        }
    }
}

/**
 * Диалог с подробной информацией об ошибке
 */
@Composable
fun ErrorDetailsDialog(
    error: DomainException,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedMessage by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Информация об ошибке")
        },
        text = {
            Column {
                Text(error.detailedMessage)
                
                if (error.cause != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Причина: ${error.cause?.message ?: "Неизвестна"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                
                if (showCopiedMessage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Текст скопирован в буфер обмена",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { 
                        clipboardManager.setText(AnnotatedString(error.detailedMessage))
                        showCopiedMessage = true
                    }
                ) {
                    Text("Копировать")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        }
    )
}
