package ru.izhxx.aichallenge.features.mcp.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * Экран для подключения к MCP и отображения списка инструментов.
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
            text = "MCP инструменты",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.url,
            onValueChange = { viewModel.onEvent(McpEvent.UrlChanged(it)) },
            label = { Text("WebSocket URL MCP (например, ws://localhost:3000/mcp)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.loading
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.onEvent(McpEvent.Load) },
            enabled = !state.loading
        ) {
            Text("Загрузить инструменты")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.error != null) {
            Text(
                text = state.error ?: "",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            if (state.tools.isEmpty()) {
                Text("Инструменты не найдены")
            } else {
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
