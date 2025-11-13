package ru.izhxx.aichallenge.features.chat.presentation.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Компонент для отображения текста с возможностью выделения
 * Делает текст выделяемым и копируемым
 */
@Composable
fun SelectableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onLongClick: (() -> Unit)? = null
) {
    SelectionContainer {
        BasicText(
            text = AnnotatedString(text),
            modifier = modifier.then(
                if (onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongClick() }
                        )
                    }
                } else {
                    Modifier
                }
            ),
            style = style,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}
