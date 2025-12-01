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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
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
import ru.izhxx.aichallenge.domain.model.config.ResponseFormat

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
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = AppDimens.baseContentPadding)
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
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null
                            )
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

                // Секция maxTokens
                Text(
                    text = "Максимум токенов",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Максимальное количество токенов в ответе:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = state.maxTokens,
                    onValueChange = { viewModel.updateMaxTokens(it) },
                    label = { Text("Max Tokens") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
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
                                ResponseFormat.JSON -> "JSON формат"
                                ResponseFormat.MARKDOWN -> "Markdown"
                                ResponseFormat.PLAIN -> "Простой текст"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ============ ИСПОЛЬЗОВАНИЕ MCP TOOLS ============
                Text(
                    text = "MCP tools (function calling)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Позволяет LLM вызывать инструменты через MCP (function calling). Требуется корректно настроенное MCP-подключение.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Использовать MCP tools",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = state.enableMcpToolCalling,
                        onCheckedChange = { viewModel.updateEnableMcpToolCalling(it) },
                        enabled = !state.isLoading
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ============ RAG НАСТРОЙКИ ============
                Text(
                    text = "RAG (Retrieval-Augmented Generation)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Включите RAG и укажите путь к JSON-индексу, созданному doc-indexer. При включении RAG запросы будут дополняться релевантным контекстом из индекса.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Переключатель RAG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Включить RAG",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = state.ragEnabled,
                        onCheckedChange = { viewModel.updateRagEnabled(it) },
                        enabled = !state.isLoading
                    )
                }

                // Путь к индексу
                OutlinedTextField(
                    value = state.ragIndexPath,
                    onValueChange = { viewModel.updateRagIndexPath(it) },
                    label = { Text("Путь к индексу (JSON)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.ragEnabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Параметры ретрива
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-K
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RAG Top-K",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = state.ragTopK,
                            onValueChange = { viewModel.updateRagTopK(it) },
                            label = { Text("Top-K") },
                            enabled = !state.isLoading && state.ragEnabled,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Min Score
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RAG Min score (0.0 - 1.0)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = state.ragMinScore,
                            onValueChange = { viewModel.updateRagMinScore(it) },
                            label = { Text("Min score") },
                            enabled = !state.isLoading && state.ragEnabled,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Max context tokens
                Text(
                    text = "RAG Max context tokens",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = state.ragMaxContextTokens,
                    onValueChange = { viewModel.updateRagMaxContextTokens(it) },
                    label = { Text("Max context tokens") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.ragEnabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ===== Reranker (2-й этап) =====
                Text(
                    text = "Reranker (2-й этап)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Индикатор режима
                Text(
                    text = if (state.ragEnabled) {
                        val modeStr = when (state.ragRerankMode) {
                            ru.izhxx.aichallenge.domain.rag.RerankMode.None -> "None"
                            ru.izhxx.aichallenge.domain.rag.RerankMode.MMR -> "MMR"
                            ru.izhxx.aichallenge.domain.rag.RerankMode.LLM -> "LLM"
                        }
                        "RAG: On  •  Rerank: $modeStr"
                    } else {
                        "RAG: Off"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Переключатель режима Rerank
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.ragRerankMode == ru.izhxx.aichallenge.domain.rag.RerankMode.None,
                        onClick = { viewModel.updateRagRerankMode(ru.izhxx.aichallenge.domain.rag.RerankMode.None) },
                        enabled = !state.isLoading && state.ragEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("None", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = state.ragRerankMode == ru.izhxx.aichallenge.domain.rag.RerankMode.MMR,
                        onClick = { viewModel.updateRagRerankMode(ru.izhxx.aichallenge.domain.rag.RerankMode.MMR) },
                        enabled = !state.isLoading && state.ragEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MMR", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = state.ragRerankMode == ru.izhxx.aichallenge.domain.rag.RerankMode.LLM,
                        onClick = { viewModel.updateRagRerankMode(ru.izhxx.aichallenge.domain.rag.RerankMode.LLM) },
                        enabled = !state.isLoading && state.ragEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LLM", style = MaterialTheme.typography.bodyMedium)
                }

                // Параметры MMR
                if (state.ragRerankMode == ru.izhxx.aichallenge.domain.rag.RerankMode.MMR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "candidateK",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = state.ragCandidateK,
                                onValueChange = { viewModel.updateRagCandidateK(it) },
                                label = { Text("candidateK") },
                                enabled = !state.isLoading && state.ragEnabled,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "mmrLambda (0.0 - 1.0)",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = state.ragMmrLambda,
                                onValueChange = { viewModel.updateRagMmrLambda(it) },
                                label = { Text("mmrLambda") },
                                enabled = !state.isLoading && state.ragEnabled,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cutoff режим
                Text(
                    text = "Cutoff режим",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.ragCutoffMode == ru.izhxx.aichallenge.domain.rag.CutoffMode.Static,
                        onClick = { viewModel.updateRagCutoffMode(ru.izhxx.aichallenge.domain.rag.CutoffMode.Static) },
                        enabled = !state.isLoading && state.ragEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Static", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = state.ragCutoffMode == ru.izhxx.aichallenge.domain.rag.CutoffMode.Quantile,
                        onClick = { viewModel.updateRagCutoffMode(ru.izhxx.aichallenge.domain.rag.CutoffMode.Quantile) },
                        enabled = !state.isLoading && state.ragEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quantile", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = state.ragCutoffMode == ru.izhxx.aichallenge.domain.rag.CutoffMode.ZScore,
                        onClick = { viewModel.updateRagCutoffMode(ru.izhxx.aichallenge.domain.rag.CutoffMode.ZScore) },
                        enabled = !state.isLoading && state.ragEnabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Z‑Score", style = MaterialTheme.typography.bodyMedium)
                }

                // Поля для конкретного cutoff
                when (state.ragCutoffMode) {
                    ru.izhxx.aichallenge.domain.rag.CutoffMode.Static -> {
                        Text(
                            text = "minRerankScore (0.0 - 1.0)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = state.ragMinRerankScore,
                            onValueChange = { viewModel.updateRagMinRerankScore(it) },
                            label = { Text("minRerankScore") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading && state.ragEnabled,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    ru.izhxx.aichallenge.domain.rag.CutoffMode.Quantile -> {
                        Text(
                            text = "quantile q (0.0 - 1.0)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = state.ragQuantileQ,
                            onValueChange = { viewModel.updateRagQuantileQ(it) },
                            label = { Text("quantile q") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading && state.ragEnabled,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    ru.izhxx.aichallenge.domain.rag.CutoffMode.ZScore -> {
                        Text(
                            text = "z‑score threshold (например, -0.5)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = state.ragZScore,
                            onValueChange = { viewModel.updateRagZScore(it) },
                            label = { Text("z‑score") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading && state.ragEnabled,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ============ RAG ИНДЕКСАЦИЯ ДОКУМЕНТОВ ============
                Text(
                    text = "Индексация документов",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Выберите папку с документацией (.md файлы) и нажмите 'Проиндексировать' для создания RAG индекса.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Путь к папке с документацией
                OutlinedTextField(
                    value = state.docsDirectory,
                    onValueChange = { viewModel.updateDocsDirectory(it) },
                    label = { Text("Папка с документацией") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isIndexing,
                    singleLine = true,
                    placeholder = { Text("Например: C:/Projects/docs") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка индексации
                Button(
                    onClick = { viewModel.indexDocuments() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isIndexing && state.docsDirectory.isNotBlank()
                ) {
                    if (state.isIndexing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Индексация...")
                    } else {
                        Text("Проиндексировать")
                    }
                }

                // Прогресс индексации
                state.indexingProgress?.let { progress ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Успех индексации
                state.indexingSuccess?.let { success ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Snackbar(
                        modifier = Modifier.fillMaxWidth(),
                        action = {
                            TextButton(onClick = { viewModel.clearIndexingStatus() }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text(success)
                    }
                }

                // Ошибка индексации
                state.indexingError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Snackbar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        action = {
                            TextButton(onClick = { viewModel.clearIndexingStatus() }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text(error)
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

                // ============ СЕКЦИЯ РАСШИРЕННЫХ НАСТРОЕК LLM ============
                Text(
                    text = "Расширенные настройки LLM",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )

                Text(
                    text = "Дополнительные параметры, влияющие на генерацию ответов:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Секция topK
                Text(
                    text = "Top-K",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Количество наиболее вероятных токенов, рассматриваемых при генерации:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = state.topK,
                    onValueChange = { viewModel.updateTopK(it) },
                    label = { Text("Top-K") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Секция topP
                Text(
                    text = "Top-P (Nucleus Sampling)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Порог вероятности для выбора токенов (0.0 - 1.0):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = state.topP,
                    onValueChange = { viewModel.updateTopP(it) },
                    label = { Text("Top-P") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Секция minP
                Text(
                    text = "Min-P",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Минимальный порог вероятности для токенов (0.0 - 1.0):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = state.minP,
                    onValueChange = { viewModel.updateMinP(it) },
                    label = { Text("Min-P") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Секция topA
                Text(
                    text = "Top-A",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Параметр для выбора токенов на основе абсолютного преимущества:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = state.topA,
                    onValueChange = { viewModel.updateTopA(it) },
                    label = { Text("Top-A") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Секция seed
                Text(
                    text = "Seed",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Seed для генератора случайных чисел (для воспроизводимости):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = state.seed,
                    onValueChange = { viewModel.updateSeed(it) },
                    label = { Text("Seed") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
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
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
