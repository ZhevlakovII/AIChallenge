# План реализации экрана истории чатов

## 1. Файловая структура

### 1.1. Новые файлы

```
composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/history/
    ├── presentation/
    │   ├── ChatHistoryScreen.kt           // UI экрана истории чатов
    │   ├── ChatHistoryViewModel.kt        // ViewModel для экрана истории
    │   ├── model/
    │   │   ├── ChatHistoryState.kt        // Состояние UI экрана истории
    │   │   └── ChatHistoryEvent.kt        // События UI экрана истории
    │   └── components/
    │       └── ChatHistoryItem.kt         // Компонент элемента истории чатов
    └── di/
        └── ChatHistoryModule.kt           // Модуль внедрения зависимостей для экрана истории
```

### 1.2. Изменения в существующих файлах

- `App.kt` - добавление нового маршрута и экрана в навигацию
- `ChatScreen.kt` - добавление кнопки перехода к истории чатов
- `ChatViewModel.kt` - изменение логики для работы с выбранным диалогом

## 2. Содержимое файлов

### 2.1. ChatHistoryState.kt

```kotlin
package ru.izhxx.aichallenge.features.history.presentation.model

import ru.izhxx.aichallenge.domain.model.DialogInfo
import ru.izhxx.aichallenge.domain.model.error.DomainException

/**
 * Состояние UI экрана истории чатов
 */
data class ChatHistoryState(
    /**
     * Список диалогов
     */
    val dialogs: List<DialogInfo> = emptyList(),
    
    /**
     * Флаг загрузки
     */
    val isLoading: Boolean = true,
    
    /**
     * Ошибка, если есть
     */
    val error: DomainException? = null
)
```

### 2.2. ChatHistoryEvent.kt

```kotlin
package ru.izhxx.aichallenge.features.history.presentation.model

/**
 * События UI экрана истории чатов
 */
sealed class ChatHistoryEvent {
    /**
     * Событие выбора диалога
     */
    data class SelectDialog(val dialogId: String) : ChatHistoryEvent()
    
    /**
     * Событие удаления диалога
     */
    data class DeleteDialog(val dialogId: String) : ChatHistoryEvent()
    
    /**
     * Событие обновления списка диалогов
     */
    object RefreshDialogs : ChatHistoryEvent()
    
    /**
     * Событие создания нового диалога
     */
    object CreateNewDialog : ChatHistoryEvent()
}
```

### 2.3. ChatHistoryItem.kt

```kotlin
package ru.izhxx.aichallenge.features.history.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.izhxx.aichallenge.domain.model.DialogInfo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Элемент списка истории чатов
 */
@Composable
fun ChatHistoryItem(
    dialog: DialogInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Заголовок диалога
                Text(
                    text = dialog.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Дата и время обновления диалога
                val dateFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
                Text(
                    text = "Обновлено: ${dateFormat.format(Date(dialog.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Количество сообщений
                Text(
                    text = "Сообщений: ${dialog.messageCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Кнопка удаления
            IconButton(onClick = onDelete) {
                Text("🗑️")
            }
        }
    }
}
```

### 2.4. ChatHistoryScreen.kt

```kotlin
package ru.izhxx.aichallenge.features.history.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.features.history.presentation.components.ChatHistoryItem
import ru.izhxx.aichallenge.features.history.presentation.model.ChatHistoryEvent

/**
 * Экран истории чатов
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    onNavigateBack: () -> Unit,
    onSelectDialog: (String) -> Unit,
    viewModel: ChatHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // Показываем диалог подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dialogToDelete by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История чатов") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                },
                actions = {
                    // Кнопка обновления списка
                    IconButton(onClick = { viewModel.processEvent(ChatHistoryEvent.RefreshDialogs) }) {
                        Text("🔄")
                    }
                    // Кнопка создания нового диалога
                    IconButton(onClick = { viewModel.processEvent(ChatHistoryEvent.CreateNewDialog) }) {
                        Text("➕")
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
            if (state.isLoading) {
                // Показываем индикатор загрузки
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            } else if (state.dialogs.isEmpty()) {
                // Если список пуст, показываем сообщение
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "История чатов пуста",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.processEvent(ChatHistoryEvent.CreateNewDialog) }
                    ) {
                        Text("Создать новый диалог")
                    }
                }
            } else {
                // Показываем список диалогов
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.dialogs) { dialog ->
                        ChatHistoryItem(
                            dialog = dialog,
                            onClick = {
                                // Выбор диалога
                                viewModel.processEvent(ChatHistoryEvent.SelectDialog(dialog.id))
                            },
                            onDelete = {
                                // Запрашиваем подтверждение удаления
                                dialogToDelete = dialog.id
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            // Отображаем ошибку, если есть
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(error.message ?: "Неизвестная ошибка")
                }
            }
            
            // Диалог подтверждения удаления
            if (showDeleteDialog && dialogToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        dialogToDelete = null
                    },
                    title = {
                        Text("Подтверждение удаления")
                    },
                    text = {
                        Text("Вы уверены, что хотите удалить этот диалог? Это действие нельзя отменить.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                dialogToDelete?.let { dialogId ->
                                    viewModel.processEvent(ChatHistoryEvent.DeleteDialog(dialogId))
                                }
                                showDeleteDialog = false
                                dialogToDelete = null
                            }
                        ) {
                            Text("Удалить")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                dialogToDelete = null
                            }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }
}
```

### 2.5. ChatHistoryViewModel.kt

```kotlin
package ru.izhxx.aichallenge.features.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.error.DomainException
import ru.izhxx.aichallenge.domain.repository.DialogPersistenceRepository
import ru.izhxx.aichallenge.features.history.presentation.model.ChatHistoryEvent
import ru.izhxx.aichallenge.features.history.presentation.model.ChatHistoryState

/**
 * ViewModel для экрана истории чатов
 */
class ChatHistoryViewModel(
    private val dialogPersistenceRepository: DialogPersistenceRepository,
    private val onDialogSelected: (String) -> Unit,
    private val onNewDialogCreated: () -> Unit
) : ViewModel() {

    // Создаем логгер
    private val logger = Logger.forClass(this)
    
    // Состояние UI
    private val _state = MutableStateFlow(ChatHistoryState(isLoading = true))
    val state: StateFlow<ChatHistoryState> = _state.asStateFlow()
    
    init {
        // При создании ViewModel загружаем список диалогов
        loadDialogs()
    }
    
    /**
     * Загружает список диалогов из репозитория
     */
    private fun loadDialogs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Получаем список диалогов и сортируем по времени обновления (новые сверху)
                val dialogs = dialogPersistenceRepository.getAllDialogs()
                    .sortedByDescending { it.updatedAt }
                
                logger.d("Загружено ${dialogs.size} диалогов")
                
                _state.update { it.copy(dialogs = dialogs, isLoading = false) }
            } catch (e: Exception) {
                logger.e("Ошибка при загрузке списка диалогов", e)
                
                val error = e as? DomainException ?: DomainException(
                    "Не удалось загрузить историю чатов: ${e.message ?: "неизвестная ошибка"}",
                    e
                )
                
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }
    
    /**
     * Обрабатывает события UI
     */
    fun processEvent(event: ChatHistoryEvent) {
        viewModelScope.launch {
            when (event) {
                is ChatHistoryEvent.SelectDialog -> handleSelectDialog(event.dialogId)
                is ChatHistoryEvent.DeleteDialog -> handleDeleteDialog(event.dialogId)
                is ChatHistoryEvent.RefreshDialogs -> loadDialogs()
                is ChatHistoryEvent.CreateNewDialog -> handleCreateNewDialog()
            }
        }
    }
    
    /**
     * Обрабатывает выбор диалога из списка
     */
    private fun handleSelectDialog(dialogId: String) {
        logger.d("Выбран диалог: $dialogId")
        onDialogSelected(dialogId)
    }
    
    /**
     * Обрабатывает удаление диалога
     */
    private suspend fun handleDeleteDialog(dialogId: String) {
        logger.d("Удаление диалога: $dialogId")
        
        try {
            // Удаляем диалог
            dialogPersistenceRepository.deleteDialog(dialogId)
            
            // Обновляем список диалогов
            loadDialogs()
            
            // Показываем уведомление об успешном удалении
            _state.update { it.copy(
                error = DomainException("Диалог успешно удален", null, isError = false)
            ) }
            
            // Через 2 секунды скрываем уведомление
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _state.update { it.copy(error = null) }
            }
        } catch (e: Exception) {
            logger.e("Ошибка при удалении диалога", e)
            
            // Показываем ошибку
            val error = e as? DomainException ?: DomainException(
                "Не удалось удалить диалог: ${e.message ?: "неизвестная ошибка"}",
                e
            )
            
            _state.update { it.copy(error = error) }
        }
    }
    
    /**
     * Обрабатывает создание нового диалога
     */
    private fun handleCreateNewDialog() {
        logger.d("Создание нового диалога")
        
        // Вызываем обработчик создания нового диалога
        onNewDialogCreated()
    }
}
```

### 2.6. ChatHistoryModule.kt

```kotlin
package ru.izhxx.aichallenge.features.history.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.history.presentation.ChatHistoryViewModel

/**
 * Модуль внедрения зависимостей для экрана истории чатов
 */
val chatHistoryModule = module {
    
    /**
     * Создаем ViewModel для экрана истории чатов
     * 
     * @param onDialogSelected - обработчик выбора диалога
     * @param onNewDialogCreated - обработчик создания нового диалога
     */
    viewModel { (
        onDialogSelected: (String) -> Unit,
        onNewDialogCreated: () -> Unit
    ) ->
        ChatHistoryViewModel(
            dialogPersistenceRepository = get(),
            onDialogSelected = onDialogSelected,
            onNewDialogCreated = onNewDialogCreated
        )
    }
}
```

## 3. Модификация существующих файлов

### 3.1. Изменения в App.kt

```kotlin
// Добавить в sealed class Screen
sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object Metrics : Screen("metrics")
    object ChatHistory : Screen("chat_history") // Новый маршрут
}

// Добавить в NavHost
NavHost(navController = navController, startDestination = Screen.Chat.route) {
    // Экран чата
    composable(Screen.Chat.route) {
        ChatScreen(
            onNavigateToSettings = {
                navController.navigate(Screen.Settings.route)
            },
            onNavigateToHistory = {
                navController.navigate(Screen.ChatHistory.route)
            }
        )
    }

    // Экран истории чатов
    composable(Screen.ChatHistory.route) {
        ChatHistoryScreen(
            onNavigateBack = { navController.popBackStack() },
            onSelectDialog = { dialogId ->
                // Передаем ID выбранного диалога и возвращаемся на экран чата
                navController.navigate("${Screen.Chat.route}/$dialogId") {
                    popUpTo(Screen.Chat.route) { inclusive = true }
                }
            }
        )
    }
    
    // Остальные экраны без изменений
    // ...
}
```

### 3.2. Изменения в ChatScreen.kt

```kotlin
// Изменить определение функции
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit, // Добавить параметр
    viewModel: ChatViewModel = koinViewModel()
) {
    // ...
    
    // Изменить TopAppBar, добавив кнопку перехода к истории
    TopAppBar(
        title = { Text("Android Developer Assistant") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            // Кнопка перехода к истории чатов
            IconButton(onClick = onNavigateToHistory) {
                Text("📜")
            }
            
            // Кнопка очистки истории
            IconButton(onClick = { viewModel.processEvent(ChatEvent.ClearHistory) }) {
                Text("🗑️")
            }
            
            // Кнопка настроек
            IconButton(onClick = onNavigateToSettings) {
                Text("⚙️")
            }
        }
    )
    
    // ...
}
```

### 3.3. Изменения в ChatViewModel.kt

```kotlin
// Добавить новое событие в ChatEvent
sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    object RetryLastMessage : ChatEvent()
    object ClearHistory : ChatEvent()
    data class UpdateInputText(val text: String) : ChatEvent()
    data class LoadDialog(val dialogId: String) : ChatEvent() // Новое событие
}

// Модифицировать метод processEvent
fun processEvent(event: ChatEvent) {
    viewModelScope.launch {
        when (event) {
            is ChatEvent.SendMessage -> handleSendMessage(event.text)
            is ChatEvent.RetryLastMessage -> handleRetryLastMessage()
            is ChatEvent.ClearHistory -> handleClearHistory()
            is ChatEvent.UpdateInputText -> handleUpdateInputText(event.text)
            is ChatEvent.LoadDialog -> handleLoadDialog(event.dialogId) // Новый обработчик
        }
    }
}

// Добавить метод handleLoadDialog
private suspend fun handleLoadDialog(dialogId: String) {
    try {
        // Очищаем текущий диалог
        messageHistory.update { it.apply { clear() } }
        
        // Устанавливаем новый ID диалога
        currentDialogId = dialogId
        
        // Загружаем сообщения
        val savedMessages = dialogPersistenceRepository.getDialogMessages(dialogId)
        messageHistory.update { it.apply { addAll(savedMessages) } }
        
        // Загружаем суммаризацию
        currentSummary = dialogPersistenceRepository.getLatestSummary(dialogId)
        
        // Преобразуем сообщения в UI-модели
        val uiMessages = savedMessages.map { message ->
            when (message.role) {
                MessageRole.USER -> responseMapper.createUserUiMessage(
                    message.content, 
                    false,
                    UUID.randomUUID().toString()
                )
                MessageRole.ASSISTANT -> responseMapper.createAssistantUiMessage(
                    message.content,
                    null, // format можно определить по содержимому или хранить отдельно
                    null  // usage отсутствует при восстановлении
                )
                else -> responseMapper.createTechnicalUiMessage(message.content)
            }
        }
        
        // Обновляем UI
        _state.update { it.copy(messages = uiMessages) }
        
    } catch (e: Exception) {
        // В случае ошибки показываем сообщение и создаем новый диалог
        val errorMessage = responseMapper.createTechnicalUiMessage(
            "Не удалось загрузить диалог: ${e.message ?: "неизвестная ошибка"}"
        )
        
        // Создаем новый диалог
        currentDialogId = dialogPersistenceRepository.createNewDialog()
        messageHistory.update { it.apply { clear() } }
        currentSummary = null
        
        _state.update { it.copy(messages = listOf(errorMessage)) }
    }
}

// Модифицировать метод initializeOrRestoreDialog для поддержки загрузки диалога по ID
private suspend fun initializeOrRestoreDialog() {
    try {
        // Создаем новый диалог
        currentDialogId = dialogPersistenceRepository.createNewDialog()
        
        // Здесь в будущем можно добавить восстановление последнего диалога
    } catch (e: Exception) {
        // В случае ошибки создаем временный диалог
        e.printStackTrace()
        currentDialogId = UUID.randomUUID().toString()
    }
}
```

### 3.4. Добавление в AppModule.kt

```kotlin
// Добавить импорт нового модуля
import ru.izhxx.aichallenge.features.history.di.chatHistoryModule

// Добавить модуль в список
val appModule = module {
    // Существующие модули
    includes(
        sharedModule,
        chatModule,
        settingsModule,
        metricsModule,
        chatHistoryModule // Новый модуль
    )
}
```

## 4. Навигация и передача параметров

### 4.1. Обработка параметров в ChatScreen

```kotlin
// В NavHost внутри App.kt изменить определение маршрута чата
composable(
    route = "${Screen.Chat.route}?dialogId={dialogId}",
    arguments = listOf(
        navArgument("dialogId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry ->
    val dialogId = backStackEntry.arguments?.getString("dialogId")
    
    ChatScreen(
        onNavigateToSettings = {
            navController.navigate(Screen.Settings.route)
        },
        onNavigateToHistory = {
            navController.navigate(Screen.ChatHistory.route)
        },
        dialogId = dialogId // Передаем ID диалога, если есть
    )
}
```

### 4.2. Изменения в ChatScreen для приема dialogId

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    dialogId: String? = null,
    viewModel: ChatViewModel = koinViewModel()
) {
    // Загружаем диалог при первом запуске, если передан ID
    LaunchedEffect(dialogId) {
        if (dialogId != null) {
            viewModel.processEvent(ChatEvent.LoadDialog(dialogId))
        }
    }
    
    // Остальной код без изменений
    // ...
}
```

## 5. Тестирование

После реализации всех указанных компонентов, пользователь должен иметь возможность:
1. Видеть и выбирать сохраненные диалоги из истории
2. Удалять ненужные диалоги
3. Создавать новые диалоги
4. Легко переключаться между историей и текущим диалогом

Это позволит сделать приложение более удобным и функциональным.
