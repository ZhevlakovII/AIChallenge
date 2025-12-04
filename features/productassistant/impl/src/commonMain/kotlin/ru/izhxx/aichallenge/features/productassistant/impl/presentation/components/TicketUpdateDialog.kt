package ru.izhxx.aichallenge.features.productassistant.impl.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog for adding comments to tickets
 */
@Composable
fun AddCommentDialog(
    ticketId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmitComment: (ticketId: String, comment: String) -> Unit
) {
    var comment by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .widthIn(max = 400.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Добавить комментарий",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Тикет: ${ticketId.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий") },
                placeholder = { Text("Введите комментарий или дополнительную информацию") },
                enabled = !isLoading,
                minLines = 3,
                maxLines = 6
            )

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            if (comment.isNotBlank()) {
                                onSubmitComment(ticketId, comment)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = comment.isNotBlank()
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for updating ticket status and optionally adding comment
 */
@Composable
fun UpdateTicketDialog(
    ticketId: String,
    currentStatus: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUpdateTicket: (ticketId: String, newStatus: String, comment: String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus.lowercase()) }
    var comment by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .widthIn(max = 400.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Обновить тикет",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Тикет: ${ticketId.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status selector
            Text(
                text = "Новый статус:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            val possibleStatuses = listOf("open", "in_progress", "resolved")
            possibleStatuses.forEach { status ->
                StatusOption(
                    status = status,
                    isSelected = selectedStatus == status,
                    onSelected = { selectedStatus = it }
                )
            }

            // Optional comment
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий (опционально)") },
                placeholder = { Text("Добавьте комментарий к изменению статуса") },
                enabled = !isLoading,
                minLines = 2,
                maxLines = 4
            )

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            onUpdateTicket(ticketId, selectedStatus, comment.ifBlank { null })
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Обновить")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusOption(
    status: String,
    isSelected: Boolean,
    onSelected: (String) -> Unit
) {
    Button(
        onClick = { onSelected(status) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                getStatusColor(status)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(
            text = getStatusDisplayText(status),
            color = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "open", "открыт" -> Color(0xFFE57373)
        "in_progress", "в работе" -> Color(0xFFFFB74D)
        "resolved", "решён" -> Color(0xFF81C784)
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getStatusDisplayText(status: String): String {
    return when (status.lowercase()) {
        "open" -> "Открыт"
        "in_progress" -> "В работе"
        "resolved" -> "Решён"
        else -> status
    }
}
