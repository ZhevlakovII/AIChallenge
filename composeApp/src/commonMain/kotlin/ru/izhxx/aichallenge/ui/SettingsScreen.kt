package ru.izhxx.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.viewmodel.SettingsViewModel

/**
 * Экран настроек с вводом API ключа
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isPasswordVisible by remember { mutableStateOf(false) }
    
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
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок
            TopAppBar(
                title = { Text("Настройки API") },
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Описание
            Text(
                text = "Для использования чат-бота необходимо ввести API ключ OpenAI.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            // Поле для ввода API ключа
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("API ключ OpenAI") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                singleLine = true,
                visualTransformation = if (isPasswordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.saveApiKey() }
                ),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        // Здесь должна быть иконка для переключения видимости пароля
                        Text(if (isPasswordVisible) "Скрыть" else "Показать")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопка сохранения
            Button(
                onClick = { viewModel.saveApiKey() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && state.apiKey.isNotBlank()
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Информация о получении API ключа
            Text(
                text = "Вы можете получить API ключ на официальном сайте OpenAI: https://platform.openai.com/api-keys",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Отображение ошибок
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter),
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
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
