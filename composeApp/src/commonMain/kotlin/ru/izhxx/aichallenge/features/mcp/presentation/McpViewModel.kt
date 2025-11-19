package ru.izhxx.aichallenge.features.mcp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.mcp.domain.usecase.EnsureMcpConnectedUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpToolsUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetSavedMcpUrlUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.SaveMcpUrlUseCase

/**
 * ViewModel для экрана MCP (MVI).
 *
 * Ответственность:
 * - Хранение/редактирование MCP WebSocket URL
 * - Проверка соединения с MCP (EnsureMcpConnectedUseCase)
 * - Загрузка списка инструментов (GetMcpToolsUseCase)
 * - Сохранение URL в DataStore (SaveMcpUrlUseCase)
 */
class McpViewModel(
    private val getMcpTools: GetMcpToolsUseCase,
    private val ensureConnected: EnsureMcpConnectedUseCase,
    private val getSavedUrl: GetSavedMcpUrlUseCase,
    private val saveUrl: SaveMcpUrlUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(McpState())
    val state: StateFlow<McpState> = _state.asStateFlow()

    init {
        // Инициализация: подтянуть сохранённый URL при старте
        viewModelScope.launch {
            val saved = getSavedUrl()
            _state.update { it.copy(url = saved) }
        }
    }

    fun onEvent(event: McpEvent) {
        when (event) {
            is McpEvent.UrlChanged -> _state.update { it.copy(url = event.value) }
            McpEvent.SaveUrl -> persistUrl()
            McpEvent.CheckConnection -> checkConnection()
            McpEvent.Load -> loadTools()
        }
    }

    private fun persistUrl() {
        val url = state.value.url.trim()
        if (url.isBlank()) {
            _state.update { it.copy(error = "URL не может быть пустым") }
            return
        }
        viewModelScope.launch {
            saveUrl(url)
            _state.update { it.copy(error = null) }
        }
    }

    private fun checkConnection() {
        val url = state.value.url.trim()
        if (url.isBlank()) {
            _state.update { it.copy(error = "URL не может быть пустым") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = ensureConnected(url)
            _state.update { state ->
                result.fold(
                    onSuccess = { state.copy(loading = false, connected = true) },
                    onFailure = { e -> state.copy(loading = false, connected = false, error = e.message ?: "Не удалось подключиться к MCP") }
                )
            }
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
            // Сначала проверяем соединение
            val connect = ensureConnected(url)
            if (connect.isFailure) {
                _state.update {
                    it.copy(
                        loading = false,
                        connected = false,
                        error = connect.exceptionOrNull()?.message ?: "Не удалось подключиться к MCP"
                    )
                }
                return@launch
            } else {
                _state.update { it.copy(connected = true) }
            }

            // Затем грузим список инструментов
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
