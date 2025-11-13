package ru.izhxx.aichallenge.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatUiMessage
import ru.izhxx.aichallenge.features.chat.presentation.model.MessageContent
import ru.izhxx.aichallenge.features.chat.presentation.model.MessageMetadata

/**
 * Компонент для отображения сообщения в чате
 * @param message модель сообщения
 * @param onRetry коллбэк для повторной отправки сообщения (только для пользовательских сообщений)
 */
@Composable
fun MessageItem(
    message: ChatUiMessage,
    onRetry: () -> Unit = {}
) {
    val (backgroundColor, alignment, borderColor) = when (message) {
        is ChatUiMessage.UserMessage -> {
            Triple(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                Alignment.CenterEnd,
                null // Нет обводки для пользовательских сообщений
            )
        }

        is ChatUiMessage.AssistantMessage -> {
            Triple(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                Alignment.CenterStart,
                null // Нет обводки для сообщений ассистента
            )
        }

        is ChatUiMessage.TechnicalMessage -> {
            Triple(
                MaterialTheme.colorScheme.surface, // Светлый фон для технического сообщения
                Alignment.Center,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) // Добавляем обводку для выделения
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 300.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    // Если это техническое сообщение, добавляем обводку
                    val cardModifier = if (borderColor != null) {
                        Modifier
                            .background(backgroundColor)
                            .fillMaxWidth()
                            // Добавляем рамку для технического сообщения
                            .padding(1.dp) // Толщина рамки
                            .background(borderColor)
                            .padding(1.dp) // Внутренний отступ после рамки
                            .padding(10.dp) // Отступы контента
                    } else {
                        Modifier
                            .background(backgroundColor)
                            .fillMaxWidth()
                            .padding(12.dp)
                    }

                    ContentRenderer(
                        content = message.content,
                        modifier = cardModifier
                    )
                }

                // Показываем кнопку повтора для пользовательских сообщений
                if (message is ChatUiMessage.UserMessage && message.isHasError) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("🔄", modifier = Modifier.padding(4.dp))
                    }
                }
            }

            // Отображаем метрики для ответов ассистента
            message.metadata?.let { metadata ->
                MetricsInfo(metadata)
            }
        }
    }
}

/**
 * Компонент для отображения метрик сообщения
 */
@Composable
private fun MetricsInfo(metadata: MessageMetadata) {
    Text(
        text = buildString {
            append("⏱️ ${String.format("%.2f", metadata.responseTimeMs / 1000.0)} с")
            append(" | ")
            append("🔤 Входные: ${metadata.tokensInput} | Выходные: ${metadata.tokensOutput} | Всего: ${metadata.tokensTotal}")
        },
        modifier = Modifier.padding(top = 4.dp).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Компонент для отображения содержимого сообщения в зависимости от его типа
 */
@Composable
private fun ContentRenderer(
    content: MessageContent,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (content) {
            is MessageContent.Plain -> {
                Text(text = content.text)
            }

            is MessageContent.Markdown -> {
                MarkdownRenderer(nodes = content.nodes)
            }

            is MessageContent.Structured -> {
                StructuredContentRenderer(content = content)
            }
        }
    }
}
