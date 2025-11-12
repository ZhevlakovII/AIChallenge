package ru.izhxx.aichallenge.features.chat.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.domain.model.MarkdownElement
import ru.izhxx.aichallenge.domain.model.MessageContent

/**
 * Основной компонент для отображения содержимого сообщения
 * Поддерживает структурированное, markdown и простое текстовое содержимое
 */
@Composable
fun MarkdownText(
    modifier: Modifier = Modifier,
    content: MessageContent,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enableSelection: Boolean = true
) {
    SelectionContainer(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (content) {
                is MessageContent.Structured -> {
                    StructuredMessageView(content = content, color = color)
                }

                is MessageContent.Markdown -> {
                    for (element in content.elements) {
                        MarkdownElementComposable(element = element, color = color)
                    }
                }

                is MessageContent.Plain -> {
                    Text(
                        text = content.text,
                        color = color,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Компонент для отображения структурированного содержимого
 */
@Composable
private fun StructuredMessageView(
    modifier: Modifier = Modifier,
    content: MessageContent.Structured,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Summary
        if (content.summary.items.isNotEmpty()) {
            Text(
                text = "Резюме:",
                fontWeight = FontWeight.Bold,
                fontSize = LocalTextStyle.current.fontSize * 1.1,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ParagraphComponent(items = content.summary.items, color = color)
            Box(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Explanation
        if (content.explanation.items.isNotEmpty()) {
            Text(
                text = "Подробно:",
                fontWeight = FontWeight.Bold,
                fontSize = LocalTextStyle.current.fontSize * 1.1,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
            )
            ParagraphComponent(items = content.explanation.items, color = color)
            Box(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Code
        content.code?.let { codeBlock ->
            Text(
                text = "Код:",
                fontWeight = FontWeight.Bold,
                fontSize = LocalTextStyle.current.fontSize * 1.1,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
            )
            CodeBlockComponent(code = codeBlock.code, language = codeBlock.language)
            Box(modifier = Modifier.padding(vertical = 8.dp))
        }

        // References
        if (content.references.isNotEmpty()) {
            Text(
                text = "Источники:",
                fontWeight = FontWeight.Bold,
                fontSize = LocalTextStyle.current.fontSize * 1.1,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
            )
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

/**
 * Компонент для отображения отдельного markdown элемента
 */
@Composable
private fun MarkdownElementComposable(
    modifier: Modifier = Modifier,
    element: MarkdownElement,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    when (element) {
        is MarkdownElement.Text -> {
            Text(
                text = element.content,
                color = color,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }

        is MarkdownElement.Bold -> {
            Text(
                text = element.content,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }

        is MarkdownElement.Italic -> {
            Text(
                text = element.content,
                color = color,
                fontStyle = FontStyle.Italic,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }

        is MarkdownElement.InlineCode -> {
            Text(
                text = element.content,
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace,
                fontSize = LocalTextStyle.current.fontSize * 0.9,
                modifier = modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        is MarkdownElement.Link -> {
            Text(
                text = element.text,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }

        is MarkdownElement.CodeBlock -> {
            CodeBlockComponent(code = element.code, language = element.language)
        }

        is MarkdownElement.Heading -> {
            HeadingComponent(level = element.level, text = element.content)
        }

        is MarkdownElement.UnorderedList -> {
            UnorderedListComponent(items = element.items)
        }

        is MarkdownElement.OrderedList -> {
            OrderedListComponent(items = element.items)
        }

        is MarkdownElement.Quote -> {
            QuoteComponent(text = element.content)
        }

        is MarkdownElement.Paragraph -> {
            // Обычно параграф содержит только текст
            if (element.items.isNotEmpty() && element.items[0] is MarkdownElement.Text) {
                Text(
                    text = (element.items[0] as MarkdownElement.Text).content,
                    color = color,
                    modifier = modifier.padding(vertical = 2.dp)
                )
            } else {
                ParagraphComponent(items = element.items, color = color)
            }
        }
    }
}

/**
 * Компонент для отображения кодового блока
 */
@Composable
private fun CodeBlockComponent(
    modifier: Modifier = Modifier,
    code: String,
    language: String = "",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = LocalTextStyle.current.fontSize * 0.85,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Компонент для отображения заголовков
 */
@Composable
private fun HeadingComponent(
    level: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    val fontSize = when (level) {
        1 -> LocalTextStyle.current.fontSize * 1.8
        2 -> LocalTextStyle.current.fontSize * 1.5
        3 -> LocalTextStyle.current.fontSize * 1.3
        4 -> LocalTextStyle.current.fontSize * 1.15
        else -> LocalTextStyle.current.fontSize * 1.0
    }

    val fontWeight = when (level) {
        1 -> FontWeight.Bold
        else -> FontWeight.SemiBold
    }

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/**
 * Компонент для отображения маркированного списка
 */
@Composable
private fun UnorderedListComponent(
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        for (item in items) {
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "•",
                    modifier = Modifier.padding(end = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                Text(text = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Компонент для отображения нумерованного списка
 */
@Composable
private fun OrderedListComponent(
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        for ((index, item) in items.withIndex()) {
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "${index + 1}.",
                    modifier = Modifier.padding(end = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                Text(text = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Компонент для отображения цитаты
 */
@Composable
private fun QuoteComponent(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * Компонент для отображения параграфа с inline элементами
 */
@Composable
private fun ParagraphComponent(
    modifier: Modifier = Modifier,
    items: List<MarkdownElement>,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    // Используем Row вместо аннотированной строки для корректного отображения InlineCode с фоном
    Row(modifier = modifier.fillMaxWidth()) {
        // Для хранения компонуемых элементов в строке
        Column(modifier = Modifier.weight(1f)) {
            // Временная строка для хранения текущей строки элементов
            var currentRowItems = mutableListOf<@Composable () -> Unit>()
            
            // Обрабатываем каждый элемент
            for (item in items) {
                when (item) {
                    is MarkdownElement.Text -> {
                        currentRowItems.add {
                            Text(
                                text = item.content,
                                color = color,
                            )
                        }
                    }

                    is MarkdownElement.Bold -> {
                        currentRowItems.add {
                            Text(
                                text = item.content,
                                color = color,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    is MarkdownElement.Italic -> {
                        currentRowItems.add {
                            Text(
                                text = item.content,
                                color = color,
                                fontStyle = FontStyle.Italic,
                            )
                        }
                    }

                    is MarkdownElement.InlineCode -> {
                        currentRowItems.add {
                            Text(
                                text = item.content,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = FontFamily.Monospace,
                                fontSize = LocalTextStyle.current.fontSize * 0.9,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 0.dp)
                            )
                        }
                    }

                    is MarkdownElement.Link -> {
                        currentRowItems.add {
                            Text(
                                text = item.text,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    else -> {}
                }
            }
            
            // Отображаем все элементы в одной строке
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                for (composable in currentRowItems) {
                    composable()
                }
            }
        }
    }
}
