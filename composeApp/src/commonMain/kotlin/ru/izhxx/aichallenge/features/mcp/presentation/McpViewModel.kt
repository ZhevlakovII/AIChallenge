package ru.izhxx.aichallenge.features.mcp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig
import ru.izhxx.aichallenge.mcp.domain.model.McpTool
import ru.izhxx.aichallenge.mcp.domain.usecase.EnsureMcpConnectedUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpServersUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpToolsUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.SaveMcpServersUseCase

/**
 * ViewModel для экрана MCP (MVI) с поддержкой двух MCP-серверов (минимальный UI).
 *
 * Ответственность:
 * - Хранение/редактирование двух MCP WebSocket URL
 * - Проверка соединения с MCP для обоих серверов
 * - Загрузка объединённого списка инструментов
 * - Сохранение списка серверов в DataStore
 */
class McpViewModel(
    private val getMcpTools: GetMcpToolsUseCase,
    private val ensureConnected: EnsureMcpConnectedUseCase,
    private val getServers: GetMcpServersUseCase,
    private val saveServers: SaveMcpServersUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(McpState())
    val state: StateFlow<McpState> = _state.asStateFlow()

    init {
        // Инициализация: подтянуть сохранённые сервера при старте
        viewModelScope.launch {
            val servers = runCatching { getServers() }.getOrDefault(emptyList())
            if (servers.isEmpty()) {
                // Используем значения по умолчанию из McpState
                return@launch
            }
            val s1 = servers.getOrNull(0)
            val s2 = servers.getOrNull(1)
            _state.update {
                it.copy(
                    url1 = s1?.url ?: it.url1,
                    url2 = s2?.url ?: it.url2
                )
            }
        }
    }

    fun onEvent(event: McpEvent) {
        when (event) {
            is McpEvent.Url1Changed -> _state.update { it.copy(url1 = event.value) }
            is McpEvent.Url2Changed -> _state.update { it.copy(url2 = event.value) }
            McpEvent.SaveServers -> persistServers()
            McpEvent.CheckConnections -> checkConnections()
            McpEvent.LoadToolsUnion -> loadToolsUnion()
        }
    }

    private fun persistServers() {
        val url1 = state.value.url1.trim()
        val url2 = state.value.url2.trim()

        if (url1.isBlank() && url2.isBlank()) {
            _state.update { it.copy(error = "Введите хотя бы один MCP URL") }
            return
        }
        _state.update { it.copy(error = null) }

        viewModelScope.launch {
            val list = buildList {
                if (url1.isNotBlank()) add(McpServerConfig(id = "primary", name = "Primary", url = url1))
                if (url2.isNotBlank()) add(McpServerConfig(id = "secondary", name = "Secondary", url = url2))
            }
            saveServers(list)
        }
    }

    private fun checkConnections() {
        val url1 = state.value.url1.trim()
        val url2 = state.value.url2.trim()

        if (url1.isBlank() && url2.isBlank()) {
            _state.update { it.copy(error = "Введите хотя бы один MCP URL") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r1 = if (url1.isNotBlank()) ensureConnected(url1) else Result.success(Unit)
            val r2 = if (url2.isNotBlank()) ensureConnected(url2) else Result.success(Unit)

            _state.update {
                it.copy(
                    loading = false,
                    connected1 = r1.isSuccess && url1.isNotBlank(),
                    connected2 = r2.isSuccess && url2.isNotBlank(),
                    error = when {
                        r1.isFailure && url1.isNotBlank() -> r1.exceptionOrNull()?.message
                        r2.isFailure && url2.isNotBlank() -> r2.exceptionOrNull()?.message
                        else -> null
                    }
                )
            }
        }
    }

    private fun loadToolsUnion() {
        val url1 = state.value.url1.trim()
        val url2 = state.value.url2.trim()

        if (url1.isBlank() && url2.isBlank()) {
            _state.update { it.copy(error = "Введите хотя бы один MCP URL") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            // Проверяем соединения (не блокируя, собираем результаты)
            val res = listOf(url1, url2)
                .filter { it.isNotBlank() }
                .map { url ->
                    async {
                        ensureConnected(url) to url
                    }
                }.awaitAll()

            // Если все проверки провалились — показать ошибку
            if (res.isNotEmpty() && res.all { it.first.isFailure }) {
                val msg = res.first().first.exceptionOrNull()?.message ?: "Не удалось подключиться к MCP"
                _state.update {
                    it.copy(
                        loading = false,
                        connected1 = false,
                        connected2 = false,
                        error = msg
                    )
                }
                return@launch
            }

            // Обновим connected-флаги
            val c1 = res.find { it.second == url1 }?.first?.isSuccess ?: false
            val c2 = res.find { it.second == url2 }?.first?.isSuccess ?: false
            _state.update { it.copy(connected1 = c1 && url1.isNotBlank(), connected2 = c2 && url2.isNotBlank()) }

            // Грузим инструменты с каждого доступного сервера и объединяем (distinct по имени)
            val tools = buildList {
                val urls = buildList {
                    if (url1.isNotBlank()) add(url1)
                    if (url2.isNotBlank()) add(url2)
                }
                urls.forEach { u ->
                    val r = getMcpTools(u)
                    r.onSuccess { addAll(it) }
                }
            }
            val union: List<McpTool> = tools.distinctBy { it.name }

            _state.update {
                it.copy(
                    loading = false,
                    tools = union
                )
            }
        }
    }
}
