package ru.izhxx.aichallenge.features.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.AppDimens
import ru.izhxx.aichallenge.features.reminder.presentation.model.EditorUiState
import ru.izhxx.aichallenge.features.reminder.presentation.model.PeriodUi
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderEvent
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderResultUi
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderUi

/**
 * Экран Reminder: список задач, редактор и результаты.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: ReminderViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.processEvent(ReminderEvent.Load)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Reminder") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            actions = {
                Button(onClick = { viewModel.processEvent(ReminderEvent.OpenCreate) }) {
                    Text("Новая задача")
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.baseContentPadding)
        ) {
            // Список задач
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text("Задачи", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    TasksList(
                        tasks = state.tasks,
                        onToggle = { id, enabled ->
                            viewModel.processEvent(
                                ReminderEvent.Toggle(
                                    id,
                                    enabled
                                )
                            )
                        },
                        onEdit = { id -> viewModel.processEvent(ReminderEvent.OpenEdit(id)) },
                        onDelete = { id -> viewModel.processEvent(ReminderEvent.Delete(id)) },
                        onRunNow = { id -> viewModel.processEvent(ReminderEvent.RunNow(id)) },
                        onSelect = { id -> viewModel.processEvent(ReminderEvent.SelectTask(id)) },
                        selectedTaskId = state.selectedTaskId
                    )
                }
            }

            Spacer(Modifier.padding(horizontal = 8.dp))

            // Результаты
            Card(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text("Результаты", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    ResultsList(results = state.resultsForSelected)
                }
            }
        }
    }

    // Редактор
    state.editor?.let { editor ->
        EditorPanel(
            editor = editor,
            onChange = { updated ->
                // Простой стейт: обновляется через закрытие/открытие по Save/Cancel
            },
            onSave = { st ->
                viewModel.processEvent(
                    ReminderEvent.Save(
                        id = st.id,
                        name = st.name,
                        systemPrompt = st.systemPrompt,
                        userPrompt = st.userPrompt,
                        periodUnit = st.periodUnit,
                        periodValue = st.periodValue,
                        enabled = st.enabled
                    )
                )
            },
            onCancel = { viewModel.processEvent(ReminderEvent.CloseEditor) }
        )
    }
}

@Composable
private fun TasksList(
    tasks: List<ReminderUi>,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRunNow: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    selectedTaskId: Long?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(tasks) { task ->
            val selected = task.id == selectedTaskId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(task.name, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Вкл.")
                            Checkbox(
                                checked = task.enabled,
                                onCheckedChange = { onToggle(task.id, it) })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Период: ${task.periodValue} ${task.periodUnit}")
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSelect(task.id) }) { Text("Открыть") }
                        Button(onClick = { onRunNow(task.id) }) { Text("Запустить") }
                        Button(onClick = { onEdit(task.id) }) { Text("Редактировать") }
                        Button(onClick = { onDelete(task.id) }) { Text("Удалить") }
                    }
                    if (task.lastRunAt != null || task.nextRunAt != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Последний запуск: ${task.lastRunAt ?: "-"}; Следующий: ${task.nextRunAt ?: "-"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsList(results: List<ReminderResultUi>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(results) { r ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(
                        "#${r.id} • ${r.status} • ${r.runAt}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(r.preview.ifBlank { r.errorMessage ?: "" })
                }
            }
        }
    }
}

@Composable
private fun EditorPanel(
    editor: EditorUiState,
    onChange: (EditorUiState) -> Unit,
    onSave: (EditorUiState) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(editor.name) }
    var systemPrompt by remember { mutableStateOf(editor.systemPrompt) }
    var userPrompt by remember { mutableStateOf(editor.userPrompt) }
    var periodValue by remember { mutableLongStateOf(editor.periodValue.toLong()) }
    var periodUnit by remember { mutableStateOf(editor.periodUnit) }
    var enabled by remember { mutableStateOf(editor.enabled) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    if (editor.id == null) "Новая задача" else "Редактирование задачи #${editor.id}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    label = { Text("User Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = periodValue.toString(),
                        onValueChange = { v ->
                            periodValue = v.toLongOrNull()?.coerceAtLeast(1) ?: 1
                        },
                        label = { Text("Период") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.height(8.dp))
                    PeriodSelector(
                        value = periodUnit,
                        onChange = { periodUnit = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Включена")
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onCancel() }) { Text("Отмена") }
                    Button(onClick = {
                        onSave(
                            editor.copy(
                                name = name,
                                systemPrompt = systemPrompt,
                                userPrompt = userPrompt,
                                periodUnit = periodUnit,
                                periodValue = periodValue.toInt(),
                                enabled = enabled
                            )
                        )
                    }) { Text("Сохранить") }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    value: PeriodUi,
    onChange: (PeriodUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(PeriodUi.MINUTES, PeriodUi.HOURS, PeriodUi.DAYS).forEach { v ->
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onChange(v) },
                enabled = value != v
            ) {
                Text(v.name)
            }
        }
    }
}
