package ru.izhxx.aichallenge.features.productassistant.impl.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.components.AddCommentDialog
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.components.TicketCardWithActions
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.components.TicketCreationForm
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.components.UpdateTicketDialog
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.AssistantResponseUi
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.FaqItemUi
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantEffect
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantIntent
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.TicketUi

/**
 * Main screen for Product Assistant feature
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAssistantScreen(
    viewModel: ProductAssistantViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // Dialog states
    var showCommentDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedTicketId by remember { mutableStateOf("") }
    var focusOnTitle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProductAssistantEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is ProductAssistantEffect.NavigateToTicket -> {
                    // Handle navigation to ticket details
                }
                is ProductAssistantEffect.ScrollToResponse -> {
                    // Handle scrolling to response
                }
                ProductAssistantEffect.ClearTicketForm -> {
                    // Clear form fields handled by state
                }
                ProductAssistantEffect.FocusOnTicketTitle -> {
                    focusOnTitle = true
                }
                ProductAssistantEffect.HideTicketCreationForm -> {
                    drawerState.close()
                }
                ProductAssistantEffect.ShowTicketCreationForm -> {
                    drawerState.open()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TicketCreationForm(
                    title = state.ticketTitle,
                    description = state.ticketDescription,
                    tags = state.ticketTags,
                    isLoading = state.isLoading,
                    enabled = state.isInputEnabled,
                    onTitleChanged = { title ->
                        viewModel.accept(ProductAssistantIntent.TicketTitleChanged(title))
                    },
                    onDescriptionChanged = { description ->
                        viewModel.accept(ProductAssistantIntent.TicketDescriptionChanged(description))
                    },
                    onTagsChanged = { tags ->
                        viewModel.accept(ProductAssistantIntent.TicketTagsChanged(tags))
                    },
                    onSubmit = {
                        val tagList = state.ticketTags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        viewModel.accept(ProductAssistantIntent.CreateTicket(
                            title = state.ticketTitle,
                            description = state.ticketDescription,
                            tags = tagList
                        ))
                    },
                    onCancel = {
                        viewModel.accept(ProductAssistantIntent.HideCreateTicketForm)
                    },
                    focusOnTitle = focusOnTitle,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Product Assistant") }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.accept(ProductAssistantIntent.ShowCreateTicketForm)
                    }
                ) {
                    Text("+")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Ticket creation form (shown inline when needed)
                if (state.showCreateTicketForm) {
                TicketCreationForm(
                    title = state.ticketTitle,
                    description = state.ticketDescription,
                    tags = state.ticketTags,
                    isLoading = state.isLoading,
                    enabled = state.isInputEnabled,
                    onTitleChanged = { title ->
                        viewModel.accept(ProductAssistantIntent.TicketTitleChanged(title))
                    },
                    onDescriptionChanged = { description ->
                        viewModel.accept(ProductAssistantIntent.TicketDescriptionChanged(description))
                    },
                    onTagsChanged = { tags ->
                        viewModel.accept(ProductAssistantIntent.TicketTagsChanged(tags))
                    },
                    onSubmit = {
                        val tagList = state.ticketTags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        viewModel.accept(ProductAssistantIntent.CreateTicket(
                            title = state.ticketTitle,
                            description = state.ticketDescription,
                            tags = tagList
                        ))
                    },
                    onCancel = {
                        viewModel.accept(ProductAssistantIntent.HideCreateTicketForm)
                    },
                    focusOnTitle = focusOnTitle,
                    modifier = Modifier.widthIn(max = 600.dp)
                )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Mode selector
                ModeSelector(
                    selectedMode = state.selectedMode,
                    enabled = state.isInputEnabled,
                    onModeSelected = { mode ->
                        viewModel.accept(ProductAssistantIntent.ModeChanged(mode))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Query input
                QueryInput(
                    query = state.query,
                    enabled = state.isInputEnabled,
                    onQueryChanged = { query ->
                        viewModel.accept(ProductAssistantIntent.QueryChanged(query))
                    },
                    onAskClicked = {
                        viewModel.accept(ProductAssistantIntent.AskQuestion)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loading indicator
                if (state.isLoading) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Error message
                state.error?.let { error ->
                    ErrorMessage(
                        error = error,
                        onRetryClicked = {
                            viewModel.accept(ProductAssistantIntent.Retry)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Response
                state.response?.let { response ->
                    ResponseContent(
                        response = response,
                        onTicketClicked = { ticketId ->
                            viewModel.accept(ProductAssistantIntent.ViewTicket(ticketId))
                        },
                        onStatusUpdate = { ticketId, newStatus ->
                            selectedTicketId = ticketId
                            viewModel.accept(ProductAssistantIntent.UpdateTicket(
                                ticketId = ticketId,
                                newStatus = newStatus,
                                comment = null
                            ))
                        },
                        onAddComment = { ticketId ->
                            selectedTicketId = ticketId
                            showCommentDialog = true
                        },
                        onClearClicked = {
                            viewModel.accept(ProductAssistantIntent.ClearResponse)
                        }
                    )
                }
            }
        }
    }

    // Comment dialog
    if (showCommentDialog) {
        AddCommentDialog(
            ticketId = selectedTicketId,
            isLoading = state.isLoading,
            onDismiss = { showCommentDialog = false },
            onSubmitComment = { ticketId, comment ->
                viewModel.accept(ProductAssistantIntent.UpdateTicket(
                    ticketId = ticketId,
                    newStatus = null,
                    comment = comment
                ))
                showCommentDialog = false
            }
        )
    }

    // Update dialog (if needed for full ticket updates)
    if (showUpdateDialog) {
        UpdateTicketDialog(
            ticketId = selectedTicketId,
            currentStatus = "open", // This would come from ticket details
            isLoading = state.isLoading,
            onDismiss = { showUpdateDialog = false },
            onUpdateTicket = { ticketId, newStatus, comment ->
                viewModel.accept(ProductAssistantIntent.UpdateTicket(
                    ticketId = ticketId,
                    newStatus = newStatus,
                    comment = comment
                ))
                showUpdateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeSelector(
    selectedMode: AssistantMode,
    enabled: Boolean,
    onModeSelected: (AssistantMode) -> Unit
) {
    Column {
        Text(
            text = "Режим работы",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistantMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.toDisplayString()) },
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun QueryInput(
    query: String,
    enabled: Boolean,
    onQueryChanged: (String) -> Unit,
    onAskClicked: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Задайте вопрос") },
            placeholder = { Text("Например: Почему не работает авторизация?") },
            enabled = enabled,
            minLines = 3,
            maxLines = 5
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAskClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && query.isNotBlank()
        ) {
            Text("Спросить")
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Генерирую ответ...")
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetryClicked: () -> Unit
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onRetryClicked) {
                Text("Повторить")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResponseContent(
    response: AssistantResponseUi,
    onTicketClicked: (String) -> Unit,
    onStatusUpdate: (String, String) -> Unit = { _, _ -> },
    onAddComment: (String) -> Unit = { },
    onClearClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ответ (${response.mode.toDisplayString()})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Уверенность: ${(response.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { response.confidence.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main answer
            Text(
                text = response.answer,
                style = MaterialTheme.typography.bodyMedium
            )

            // Related tickets
            if (response.relatedTickets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Связанные тикеты:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                response.relatedTickets.forEach { ticket ->
                    TicketCardWithActions(
                        ticketId = ticket.id,
                        title = ticket.title,
                        description = ticket.description,
                        status = ticket.status,
                        statusColor = ticket.statusColor,
                        tags = ticket.tags,
                        createdAt = ticket.createdAt,
                        onTicketClicked = onTicketClicked,
                        onStatusUpdate = onStatusUpdate,
                        onAddComment = onAddComment
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Related documentation
            if (response.relatedDocumentation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Релевантная документация:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                response.relatedDocumentation.forEach { faq ->
                    FaqCard(faq)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Sources
            if (response.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Источники:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    response.sources.forEach { source ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${source.typeDisplayName}: ${source.reference}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClearClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Очистить")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TicketCard(
    ticket: TicketUi,
    onTicketClicked: (String) -> Unit
) {
    Card(
        onClick = { onTicketClicked(ticket.id) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ticket.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(ticket.statusColor))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ticket.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            if (ticket.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ticket.tags.forEach { tag ->
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
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = faq.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = faq.answer,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3
            )
        }
    }
}
