package ru.izhxx.aichallenge.features.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.features.chat.presentation.components.ErrorBanner
import ru.izhxx.aichallenge.features.chat.presentation.components.ErrorDetailsDialog
import ru.izhxx.aichallenge.features.chat.presentation.components.LoadingIndicator
import ru.izhxx.aichallenge.features.chat.presentation.components.MessageItem
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatEvent

/**
 * Главный экран чата
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    var showErrorDialog by remember { mutableStateOf(false) }
    
    // Автоматическая прокрутка к последнему сообщению при добавлении нового
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Заголовок чата
            TopAppBar(
                title = { Text("Android Developer Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Кнопка очистки истории
                    IconButton(
                        onClick = { viewModel.processEvent(ChatEvent.ClearHistory) }
                    ) {
                        Text("🗑️")
                    }
                    
                    // Кнопка настроек
                    IconButton(onClick = onNavigateToSettings) {
                        Text("⚙️")
                    }
                }
            )
            
            // Список сообщений
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = lazyListState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages) { message ->
                    MessageItem(
                        message = message,
                        onRetry = {
                            viewModel.processEvent(ChatEvent.RetryLastMessage)
                        }
                    )
                }
                
                // Индикатор загрузки
                if (state.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }
            
            // Отображение ошибки над полем ввода
            state.error?.let { error ->
                ErrorBanner(
                    error = error,
                    onShowDetails = { showErrorDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
                
                // Показываем диалог с подробностями ошибки
                if (showErrorDialog) {
                    ErrorDetailsDialog(
                        error = error,
                        onDismiss = { showErrorDialog = false }
                    )
                }
            }
            
            // Поле ввода и кнопка отправки
            MessageInput(
                value = state.inputText,
                onValueChange = { viewModel.processEvent(ChatEvent.UpdateInputText(it)) },
                onSendClick = { viewModel.processEvent(ChatEvent.SendMessage(state.inputText)) },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

/**
 * Компонент для ввода сообщения
 */
@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Введите вопрос...") },
            enabled = !isLoading
        )
        
        Spacer(Modifier.width(8.dp))
        
        Button(
            onClick = onSendClick,
            enabled = value.isNotBlank() && !isLoading
        ) {
            Text("Отправить")
        }
    }
}
