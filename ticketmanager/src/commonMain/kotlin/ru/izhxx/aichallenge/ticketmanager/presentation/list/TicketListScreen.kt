@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.ticketmanager.presentation.list

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.ListTicketsUseCase
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Экран списка тикетов поддержки
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TicketListScreen(
    onTicketClick: (String) -> Unit,
    onLLMAssistantClick: () -> Unit,
    listTicketsUseCase: ListTicketsUseCase = koinInject()
) {
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Загрузка тикетов при старте и при изменении фильтров
    LaunchedEffect(selectedStatus, selectedTag) {
        isLoading = true
        error = null

        val result = listTicketsUseCase(
            statusFilter = selectedStatus,
            tagFilter = selectedTag
        )

        if (result.isSuccess) {
            tickets = result.getOrThrow()
        } else {
            error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
        }

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Список тикетов") },
                actions = {
                    // Кнопка к LLM ассистенту
                    Button(
                        onClick = onLLMAssistantClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("LLM Ассистент")
                    }
                }
            )
        },
        floatingActionButton = {
            // Кнопка обновления
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = null
                        val result = listTicketsUseCase(
                            statusFilter = selectedStatus,
                            tagFilter = selectedTag
                        )
                        if (result.isSuccess) {
                            tickets = result.getOrThrow()
                        } else {
                            error = result.exceptionOrNull()?.message
                        }
                        isLoading = false
                    }
                }
            ) {
                Text("🔄")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Фильтры
            FilterSection(
                selectedStatus = selectedStatus,
                selectedTag = selectedTag,
                onStatusChange = { selectedStatus = if (selectedStatus == it) null else it },
                onTagChange = { selectedTag = if (selectedTag == it) null else it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Контент
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    ErrorCard(
                        error = error ?: "",
                        onRetry = {
                            scope.launch {
                                isLoading = true
                                error = null
                                val result = listTicketsUseCase(
                                    statusFilter = selectedStatus,
                                    tagFilter = selectedTag
                                )
                                if (result.isSuccess) {
                                    tickets = result.getOrThrow()
                                } else {
                                    error = result.exceptionOrNull()?.message
                                }
                                isLoading = false
                            }
                        }
                    )
                }
                tickets.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    // Статистика
                    Text(
                        text = "Найдено тикетов: ${tickets.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Список тикетов
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tickets) { ticket ->
                            TicketCard(
                                ticket = ticket,
                                onClick = { onTicketClick(ticket.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    selectedStatus: String?,
    selectedTag: String?,
    onStatusChange: (String) -> Unit,
    onTagChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Фильтры",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Фильтр по статусу
        Text(
            text = "Статус:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            listOf("open", "in_progress", "resolved").forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusChange(status) },
                    label = { Text(getStatusDisplayName(status)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Фильтр по тегам
        Text(
            text = "Теги:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            listOf("auth", "network", "settings", "bug", "feature").forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { onTagChange(tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)
@Composable
private fun TicketCard(
    ticket: SupportTicket,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок и статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getStatusColor(ticket.status)
                ) {
                    Text(
                        text = ticket.status.toDisplayString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Описание
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Теги
            if (ticket.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Метаданные
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${ticket.id.take(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Создан: ${formatDate(ticket.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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

            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Тикеты не найдены",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Попробуйте изменить фильтры",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getStatusDisplayName(status: String): String {
    return when (status) {
        "open" -> "Открыт"
        "in_progress" -> "В работе"
        "resolved" -> "Решён"
        else -> status
    }
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
