package ru.izhxx.aichallenge.features.productassistant.impl.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Component for ticket actions and status updates
 */
@Composable
fun TicketActions(
    ticketId: String,
    currentStatus: String,
    onStatusUpdate: (ticketId: String, newStatus: String) -> Unit,
    onAddComment: (ticketId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusUpdateDropdown(
                currentStatus = currentStatus,
                onStatusSelected = { newStatus ->
                    onStatusUpdate(ticketId, newStatus)
                }
            )

            AssistChip(
                onClick = { onAddComment(ticketId) },
                label = { Text("Комментарий", style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
private fun StatusUpdateDropdown(
    currentStatus: String,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val possibleStatuses = listOf("open", "in_progress", "resolved")

    Button(
        onClick = { expanded = true },
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = getStatusColor(currentStatus)
        )
    ) {
        Text(
            text = getStatusDisplayText(currentStatus),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        possibleStatuses.forEach { status ->
            if (status != currentStatus.lowercase()) {
                DropdownMenuItem(
                    text = { Text(getStatusDisplayText(status)) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Enhanced ticket card with actions
 */
@Composable
fun TicketCardWithActions(
    ticketId: String,
    title: String,
    description: String,
    status: String,
    statusColor: Long,
    tags: List<String>,
    createdAt: String,
    onTicketClicked: (String) -> Unit,
    onStatusUpdate: (ticketId: String, newStatus: String) -> Unit,
    onAddComment: (ticketId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onTicketClicked(ticketId) },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(statusColor))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TicketTagsRow(tags = tags)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            TicketActions(
                ticketId = ticketId,
                currentStatus = status,
                onStatusUpdate = onStatusUpdate,
                onAddComment = onAddComment
            )
        }
    }
}

@Composable
private fun TicketTagsRow(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
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
