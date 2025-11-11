package ru.izhxx.aichallenge.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.AppDimens
import ru.izhxx.aichallenge.domain.model.ResponseFormat

/**
 * Экран настроек LLM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    // Эффект для возврата при успешном сохранении
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            // Сбрасываем флаг сохранения перед навигацией
            viewModel.clearSavedState()
            onNavigateBack()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок
            TopAppBar(
                title = { Text("Настройки LLM") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Используйте иконку "назад" из ваших ресурсов
                        Text("<")
                    }
                }
            )
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.baseContentPadding)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Описание
                Text(
                    text = "Настройте параметры для работы с LLM API:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                
                // ============ СЕКЦИЯ НАСТРОЕК ПРОВАЙДЕРА ============
                Text(
                    text = "Настройки провайдера LLM",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
                
                // Секция API ключа
                Text(
                    text = "API ключ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Для использования LLM необходимо указать API ключ:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Поле для ввода API ключа
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API ключ") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    visualTransformation = if (isApiKeyVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            // Здесь должна быть иконка для переключения видимости пароля
                            Text(if (isApiKeyVisible) "Скрыть" else "Показать")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Секция URL API
                Text(
                    text = "URL API",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "URL для доступа к API LLM:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = state.apiUrl,
                    onValueChange = { viewModel.updateApiUrl(it) },
                    label = { Text("URL API") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Секция модели
                Text(
                    text = "Модель LLM",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Укажите ID модели для запросов:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = state.model,
                    onValueChange = { viewModel.updateModel(it) },
                    label = { Text("Модель") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Секция Project ID
                Text(
                    text = "OpenAI Project ID",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Идентификатор проекта (опционально):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = state.openaiProject,
                    onValueChange = { viewModel.updateOpenaiProject(it) },
                    label = { Text("Project ID") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ============ СЕКЦИЯ НАСТРОЕК ВЗАИМОДЕЙСТВИЯ С LLM ============
                Text(
                    text = "Настройки взаимодействия с LLM",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
                
                // Секция температуры
                Text(
                    text = "Температура",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Температура генерации (0.0 - 1.0):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = state.temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    label = { Text("Температура") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Секция формата ответа
                Text(
                    text = "Формат ответа",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Выберите, в каком формате LLM должен возвращать ответы:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Радиокнопки для выбора формата
                ResponseFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (format == state.responseFormat),
                            onClick = { viewModel.updateResponseFormat(format) },
                            enabled = !state.isLoading
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (format) {
                                ResponseFormat.XML -> "XML формат"
                                ResponseFormat.JSON -> "JSON формат"
                                ResponseFormat.UNFORMATTED -> "Без особого форматирования"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Секция системного промпта
                Text(
                    text = "Системный промпт",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Определяет роль и поведение LLM агента:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = state.systemPrompt,
                    onValueChange = { viewModel.updateSystemPrompt(it) },
                    label = { Text("Системный промпт") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default
                    ),
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Кнопки управления настройками
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Кнопка восстановления настроек по умолчанию
                    TextButton(
                        onClick = { viewModel.restoreDefaults() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Text("Настройки по умолчанию")
                    }
                    
                    // Кнопка сохранения
                    Button(
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Сохранить")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Информация о получении API ключа
                Text(
                    text = "Вы можете получить API ключ на официальном сайте LLM API.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Отображение ошибок
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("ОК")
                    }
                }
            ) {
                Text(error)
            }
        }
        
        // Индикатор загрузки на весь экран при загрузке данных
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
