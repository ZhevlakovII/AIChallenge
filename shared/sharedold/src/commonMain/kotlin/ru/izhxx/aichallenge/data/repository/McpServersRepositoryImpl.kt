package ru.izhxx.aichallenge.data.repository

import ru.izhxx.aichallenge.data.database.AppDatabase
import ru.izhxx.aichallenge.data.database.entity.McpServerEntity
import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig
import ru.izhxx.aichallenge.domain.repository.McpServersRepository

/**
 * Хранение списка MCP-серверов в отдельной таблице Room: mcp_servers.
 */
class McpServersRepositoryImpl(
    private val database: AppDatabase
) : McpServersRepository {

    private val dao = database.mcpServerDao()

    override suspend fun getServers(): List<McpServerConfig> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun saveServers(servers: List<McpServerConfig>) {
        // Перезаписываем список серверов с сохранением стабильного порядка
        dao.clearAll()
        val entities = servers.mapIndexed { index, s ->
            McpServerEntity(
                id = s.id,
                name = s.name,
                url = s.url,
                sort_order = index
            )
        }
        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
    }

    private fun McpServerEntity.toDomain(): McpServerConfig =
        McpServerConfig(id = id, name = name, url = url)
}
