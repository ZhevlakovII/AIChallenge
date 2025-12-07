package ru.izhxx.aichallenge.features.productassistant.impl.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Form component for creating new support tickets
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TicketCreationForm(
    title: String,
    description: String,
    tags: String,
    isLoading: Boolean,
    enabled: Boolean,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onTagsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    focusOnTitle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusOnTitle) {
        if (focusOnTitle) {
            titleFocusRequester.requestFocus()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Создание тикета",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                label = { Text("Заголовок *") },
                placeholder = { Text("Краткое описание проблемы") },
                enabled = enabled && !isLoading,
                singleLine = true,
                isError = title.isBlank() && !enabled
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Описание *") },
                placeholder = { Text("Подробное описание проблемы, шаги воспроизведения") },
                enabled = enabled && !isLoading,
                minLines = 4,
                maxLines = 8,
                isError = description.isBlank() && !enabled
            )

            // Tags field
            OutlinedTextField(
                value = tags,
                onValueChange = onTagsChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Теги") },
                placeholder = { Text("auth, login, error (через запятую)") },
                enabled = enabled && !isLoading,
                singleLine = true
            )

            // Tags preview
            if (tags.isNotBlank()) {
                val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (tagList.isNotEmpty()) {
                    Text(
                        text = "Теги:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tagList.forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }
                }
            }

            // Action buttons
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Создание тикета...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = enabled
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = enabled && title.isNotBlank() && description.isNotBlank()
                    ) {
                        Text("Создать")
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
