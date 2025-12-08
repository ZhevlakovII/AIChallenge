@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.ticketmanager.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GetTicketUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.UpdateTicketUseCase
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Экран деталей тикета с возможностью редактирования
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TicketDetailsScreen(
    ticketId: String,
    onBackClick: () -> Unit,
    getTicketUseCase: GetTicketUseCase = koinInject(),
    updateTicketUseCase: UpdateTicketUseCase = koinInject()
) {
    var ticket by remember { mutableStateOf<SupportTicket?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Функция загрузки тикета
    val loadTicket = suspend {
        isLoading = true
        error = null

        val result = getTicketUseCase(ticketId)

        if (result.isSuccess) {
            ticket = result.getOrThrow()
        } else {
            error = result.exceptionOrNull()?.message ?: "Не удалось загрузить тикет"
        }

        isLoading = false
    }

    // Загрузка при старте
    LaunchedEffect(ticketId) {
        loadTicket()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали тикета") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    ErrorContent(
                        error = error ?: "",
                        onRetry = { scope.launch { loadTicket() } },
                        onBack = onBackClick
                    )
                }
                ticket != null -> {
                    TicketContent(
                        ticket = ticket!!,
                        onStatusChangeClick = { showStatusDialog = true },
                        onAddCommentClick = { showCommentDialog = true },
                        onRefresh = { scope.launch { loadTicket() } }
                    )
                }
            }
        }

        // Диалог изменения статуса
        if (showStatusDialog && ticket != null) {
            StatusChangeDialog(
                currentStatus = ticket!!.status,
                onDismiss = { showStatusDialog = false },
                onConfirm = { newStatus, comment ->
                    scope.launch {
                        val result = updateTicketUseCase(
                            ticketId = ticketId,
                            newStatus = newStatus,
                            comment = comment.takeIf { it.isNotBlank() }
                        )

                        if (result.isSuccess) {
                            showStatusDialog = false
                            loadTicket()
                        } else {
                            error = "Ошибка обновления: ${result.exceptionOrNull()?.message}"
                        }
                    }
                }
            )
        }

        // Диалог добавления комментария
        if (showCommentDialog) {
            AddCommentDialog(
                onDismiss = { showCommentDialog = false },
                onConfirm = { comment ->
                    scope.launch {
                        val result = updateTicketUseCase(
                            ticketId = ticketId,
                            newStatus = null,
                            comment = comment
                        )

                        if (result.isSuccess) {
                            showCommentDialog = false
                            loadTicket()
                        } else {
                            error = "Ошибка добавления комментария: ${result.exceptionOrNull()?.message}"
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)
@Composable
private fun TicketContent(
    ticket: SupportTicket,
    onStatusChangeClick: () -> Unit,
    onAddCommentClick: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Основная информация
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ticket.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = getStatusColor(ticket.status)
                        ) {
                            Text(
                                text = ticket.status.toDisplayString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "ID: ${ticket.id.take(8)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Описание:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = ticket.description,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (ticket.comments.isNotEmpty()) {
                        Text(
                            text = "Описание:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ticket.comments.forEach {
                                Text(
                                    text = it.content,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (ticket.tags.isNotEmpty()) {
                        Text(
                            text = "Теги:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ticket.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Создан: ${formatDate(ticket.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Обновлён: ${formatDate(ticket.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Действия
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStatusChangeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Изменить статус")
                }

                OutlinedButton(
                    onClick = onAddCommentClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Добавить комментарий")
                }

                IconButton(onClick = onRefresh) {
                    Text("🔄")
                }
            }
        }

        // Комментарии (если есть)
        // Note: комментарии есть в модели, но для упрощения пока не отображаем
        // В будущем можно добавить отображение истории
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ошибка",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onRetry) {
                        Text("Повторить")
                    }

                    OutlinedButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChangeDialog(
    currentStatus: TicketStatus,
    onDismiss: () -> Unit,
    onConfirm: (TicketStatus, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить статус") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Текущий статус: ${currentStatus.toDisplayString()}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text("Новый статус:")

                TicketStatus.entries.forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Text(
                            text = status.toDisplayString(),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus, comment) },
                enabled = selectedStatus != currentStatus
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun AddCommentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить комментарий") },
        text = {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(comment) },
                enabled = comment.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun getStatusColor(status: TicketStatus): Color {
    return when (status) {
        TicketStatus.OPEN -> Color(0xFF2196F3)
        TicketStatus.IN_PROGRESS -> Color(0xFFFF9800)
        TicketStatus.RESOLVED -> Color(0xFF4CAF50)
        TicketStatus.CLOSED -> Color(0xFF26EB9C)
    }
}

private fun formatDate(isoDate: Instant): String {
    return isoDate.toString().split("T").firstOrNull() ?: isoDate.toString()
}
