package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Таблица MCP серверов.
 */
@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    // Для стабильного порядка отображения/использования
    val sort_order: Int = 0
)
