package ru.izhxx.aichallenge.features.chat.presentation.components

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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.domain.model.markdown.InlineElement
import ru.izhxx.aichallenge.domain.model.markdown.MarkdownNode

/**
 * Компонент для отображения списка узлов Markdown
 */
@Composable
fun MarkdownRenderer(
    nodes: List<MarkdownNode>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier.fillMaxWidth()) {
        for (node in nodes) {
            MarkdownNodeRenderer(
                node = node,
                color = color,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * Компонент для отображения отдельного узла Markdown
 */
@Composable
private fun MarkdownNodeRenderer(
    node: MarkdownNode,
    color: Color,
    modifier: Modifier = Modifier
) {
    when (node) {
        is MarkdownNode.Paragraph -> {
            ParagraphRenderer(
                elements = node.elements,
                color = color,
                modifier = modifier
            )
        }
        is MarkdownNode.Heading -> {
            HeadingRenderer(
                level = node.level,
                elements = node.elements,
                color = color,
                modifier = modifier
            )
        }
        is MarkdownNode.CodeBlock -> {
            CodeBlockRenderer(
                code = node.code,
                language = node.language,
                modifier = modifier
            )
        }
        is MarkdownNode.UnorderedList -> {
            UnorderedListRenderer(
                items = node.items,
                color = color,
                modifier = modifier
            )
        }
        is MarkdownNode.OrderedList -> {
            OrderedListRenderer(
                items = node.items,
                color = color,
                modifier = modifier
            )
        }
        is MarkdownNode.ListItem -> {
            ParagraphRenderer(
                elements = node.elements,
                color = color,
                modifier = modifier
            )
        }
        is MarkdownNode.Quote -> {
            QuoteRenderer(
                elements = node.elements,
                color = color,
                modifier = modifier
            )
        }
        is MarkdownNode.HorizontalRule -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(color.copy(alpha = 0.3f))
                    .padding(vertical = 1.dp)
            )
        }
        is MarkdownNode.Divider -> {
            // Пустой разделитель (пустая строка)
            Box(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

/**
 * Компонент для отображения параграфа с inline-элементами
 */
@Composable
fun ParagraphRenderer(
    elements: List<InlineElement>,
    color: Color,
    modifier: Modifier = Modifier
) {
    InlineElementsRow(
        elements = elements, 
        color = color, 
        style = LocalTextStyle.current,
        modifier = modifier
    )
}

/**
 * Компонент для отображения заголовка с inline-элементами
 */
@Composable
private fun HeadingRenderer(
    level: Int,
    elements: List<InlineElement>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fontSize = when (level) {
        1 -> LocalTextStyle.current.fontSize * 1.8
        2 -> LocalTextStyle.current.fontSize * 1.5
        3 -> LocalTextStyle.current.fontSize * 1.3
        4 -> LocalTextStyle.current.fontSize * 1.15
        else -> LocalTextStyle.current.fontSize
    }
    
    val fontWeight = when (level) {
        1, 2 -> FontWeight.Bold
        else -> FontWeight.SemiBold
    }
    
    InlineElementsRow(
        elements = elements, 
        color = color, 
        style = LocalTextStyle.current.copy(
            fontSize = fontSize,
            fontWeight = fontWeight
        ),
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/**
 * Компонент для отображения блока кода
 */
@Composable
fun CodeBlockRenderer(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (language.isNotEmpty()) {
                Text(
                    text = language,
                    style = LocalTextStyle.current.copy(
                        fontSize = LocalTextStyle.current.fontSize * 0.8,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = LocalTextStyle.current.fontSize * 0.85,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Компонент для отображения ненумерованного списка
 */
@Composable
private fun UnorderedListRenderer(
    items: List<MarkdownNode.ListItem>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        for (item in items) {
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "•",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
                ParagraphRenderer(
                    elements = item.elements,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Компонент для отображения нумерованного списка
 */
@Composable
private fun OrderedListRenderer(
    items: List<MarkdownNode.ListItem>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        for ((index, item) in items.withIndex()) {
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "${index + 1}.",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
                ParagraphRenderer(
                    elements = item.elements,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Компонент для отображения цитаты
 */
@Composable
private fun QuoteRenderer(
    elements: List<InlineElement>,
    color: Color,
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
        
        InlineElementsRow(
            elements = elements,
            color = color.copy(alpha = 0.7f),
            style = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * Компонент для отображения строки с inline-элементами
 */
@Composable
private fun InlineElementsRow(
    elements: List<InlineElement>,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (element in elements) {
            InlineElementRenderer(
                element = element,
                color = color,
                style = style
            )
        }
    }
}

/**
 * Компонент для отображения отдельного inline-элемента
 */
@Composable
private fun InlineElementRenderer(
    element: InlineElement,
    color: Color,
    style: TextStyle
) {
    when (element) {
        is InlineElement.Text -> {
            Text(
                text = element.text,
                color = color,
                style = style
            )
        }
        is InlineElement.Bold -> {
            InlineElementsRow(
                elements = element.elements,
                color = color,
                style = style.copy(fontWeight = FontWeight.Bold)
            )
        }
        is InlineElement.Italic -> {
            InlineElementsRow(
                elements = element.elements,
                color = color,
                style = style.copy(fontStyle = FontStyle.Italic)
            )
        }
        is InlineElement.Code -> {
            Text(
                text = element.code,
                color = MaterialTheme.colorScheme.error,
                style = style.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = style.fontSize * 0.9
                ),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 0.dp)
            )
        }
        is InlineElement.Link -> {
            Text(
                text = element.text,
                color = MaterialTheme.colorScheme.primary,
                style = style
            )
        }
        is InlineElement.Image -> {
            // В этой версии просто отображаем альтернативный текст
            Text(
                text = "[Image: ${element.altText}]",
                color = MaterialTheme.colorScheme.primary,
                style = style
            )
        }
        is InlineElement.Strikethrough -> {
            InlineElementsRow(
                elements = element.elements,
                color = color.copy(alpha = 0.6f),
                style = style
            )
        }
    }
}
