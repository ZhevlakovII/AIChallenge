package ru.izhxx.aichallenge.mcp.orchestrator

import kotlinx.coroutines.flow.MutableStateFlow
import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig
import ru.izhxx.aichallenge.domain.repository.McpServersRepository
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * Реализация McpRouter с политикой "первый встретившийся инструмент побеждает".
 * На перестроении реестра опрашивает каждый сервер (tools/list) и строит карту toolName -> wsUrl.
 */
class PolicyBasedMcpRouter(
    private val mcpRepository: McpRepository,
    private val mcpServersRepository: McpServersRepository
) : McpRouter {

    // Храним актуальный слепок реестра
    // StateFlow потому что синхронизация из коробки
    private val _registry = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun rebuildRegistry(servers: List<McpServerConfig>): Map<String, String> {
        val newMap = linkedMapOf<String, String>()
        for (srv in servers) {
            val tools = mcpRepository.listTools(srv.url).getOrNull().orEmpty()
            for (t in tools) {
                // Не перезаписываем, чтобы "первый" выигрывал
                if (!newMap.containsKey(t.name)) {
                    newMap[t.name] = srv.url
                }
            }
        }
        _registry.value = newMap
        return _registry.value
    }

    override suspend fun resolve(toolName: String): String? {
        if (_registry.value.isEmpty()) {
            rebuildRegistry(mcpServersRepository.getServers())
        }
        return _registry.value[toolName]
    }

    override suspend fun registry(): Map<String, String> = _registry.value
}
