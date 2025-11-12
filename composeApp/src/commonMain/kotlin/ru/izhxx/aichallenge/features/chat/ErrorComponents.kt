package ru.izhxx.aichallenge.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.domain.model.LLMException

/**
 * Компонент для отображения ошибки над полем ввода
 * Показывает код ошибки, краткое описание и ссылку на подробности
 */
@Composable
fun ErrorBanner(
    exception: LLMException,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                clipboardManager.setText(AnnotatedString(exception.getFullErrorInfo()))
            }
            .padding(12.dp)
    ) {
        Column {
            // Код и описание ошибки
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exception.getShortErrorInfo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Ссылка на подробности
            Text(
                text = "подробнее",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(onClick = onShowDetails)
            )
        }
    }
}

/**
 * Диалог с подробной информацией об ошибке
 * Позволяет скопировать полный текст ошибки
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDetailsDialog(
    exception: LLMException,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Заголовок
            Text(
                text = "Информация об ошибке",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Разделитель
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .padding(vertical = 0.5.dp)
            )

            // Содержимое ошибки
            Text(
                text = exception.getFullErrorInfo(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(exception.getFullErrorInfo()))
                    }
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Кнопка закрытия
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Нажмите на текст для копирования",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}
