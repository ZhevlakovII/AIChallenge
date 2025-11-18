package ru.izhxx.aichallenge.features.mcp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpToolsUseCase

/**
 * ViewModel для экрана MCP (MVI).
 */
class McpViewModel(
    private val getMcpTools: GetMcpToolsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(McpState())
    val state: StateFlow<McpState> = _state.asStateFlow()

    fun onEvent(event: McpEvent) {
        when (event) {
            is McpEvent.UrlChanged -> _state.update { it.copy(url = event.value) }
            McpEvent.Load -> loadTools()
        }
    }

    private fun loadTools() {
        val url = state.value.url.trim()
        if (url.isBlank()) {
            _state.update { it.copy(error = "URL не может быть пустым") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val result = getMcpTools(url)
            _state.update {
                result.fold(
                    onSuccess = { tools -> it.copy(loading = false, tools = tools) },
                    onFailure = { e -> it.copy(loading = false, error = e.message ?: "Неизвестная ошибка") }
                )
            }
        }
    }
}
