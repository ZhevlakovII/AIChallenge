package ru.izhxx.aichallenge.features.mcp.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.AppDimens

/**
 * Экран для подключения к двум MCP-серверам и отображения объединённого списка инструментов.
 */
@Composable
fun McpScreen(
    viewModel: McpViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.baseContentPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "MCP инструменты (2 сервера)",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        // URL #1
        OutlinedTextField(
            value = state.url1,
            onValueChange = { viewModel.onEvent(McpEvent.Url1Changed(it)) },
            label = { Text("WebSocket URL MCP #1 (например, ws://localhost:8080/mcp)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.loading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // URL #2
        OutlinedTextField(
            value = state.url2,
            onValueChange = { viewModel.onEvent(McpEvent.Url2Changed(it)) },
            label = { Text("WebSocket URL MCP #2 (например, ws://localhost:8081/mcp)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.loading
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.onEvent(McpEvent.SaveServers) },
                enabled = !state.loading
            ) {
                Text("Сохранить URL-ы")
            }
            Button(
                onClick = { viewModel.onEvent(McpEvent.CheckConnections) },
                enabled = !state.loading
            ) {
                Text("Проверить соединения")
            }
            Button(
                onClick = { viewModel.onEvent(McpEvent.LoadToolsUnion) },
                enabled = !state.loading
            ) {
                Text("Загрузить инструменты")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Сервер #1: " + if (state.connected1) "Подключено" else "Не подключено",
                color = if (state.connected1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Сервер #2: " + if (state.connected2) "Подключено" else "Не подключено",
                color = if (state.connected2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
            state.tools.isEmpty() -> Text("Инструменты не найдены")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.tools) { tool ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = tool.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            tool.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            tool.inputSchema?.let { schema ->
                                Text(
                                    text = schema,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
