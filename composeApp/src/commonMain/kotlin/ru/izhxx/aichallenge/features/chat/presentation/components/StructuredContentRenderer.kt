package ru.izhxx.aichallenge.features.chat.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.features.chat.presentation.model.MessageContent

/**
 * Компонент для отображения структурированного содержимого
 */
@Composable
fun StructuredContentRenderer(
    content: MessageContent.Structured,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Секция "Резюме"
        if (content.summary.elements.isNotEmpty()) {
            SectionHeader(title = "Резюме:")
            ParagraphRenderer(
                elements = content.summary.elements,
                color = MaterialTheme.colorScheme.onSurface
            )
            SectionDivider()
        }

        // Секция "Подробно"
        if (content.explanation.elements.isNotEmpty()) {
            SectionHeader(title = "Подробно:")
            ParagraphRenderer(
                elements = content.explanation.elements,
                color = MaterialTheme.colorScheme.onSurface
            )
            SectionDivider()
        }

        // Секция "Код"
        content.code?.let { codeBlock ->
            SectionHeader(title = "Код:")
            CodeBlockRenderer(
                code = codeBlock.code,
                language = codeBlock.language
            )
            SectionDivider()
        }

        // Секция "Источники"
        if (content.references.isNotEmpty()) {
            SectionHeader(title = "Источники:")
            Column {
                for (reference in content.references) {
                    Text(
                        text = "• ${reference.text}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Заголовок секции структурированного содержимого
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = LocalTextStyle.current.fontSize * 1.1,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/**
 * Разделитель между секциями
 */
@Composable
private fun SectionDivider() {
    Box(modifier = Modifier.padding(vertical = 8.dp))
}
