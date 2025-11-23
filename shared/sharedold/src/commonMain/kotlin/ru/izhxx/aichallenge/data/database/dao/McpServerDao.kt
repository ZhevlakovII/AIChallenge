package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.data.database.entity.McpServerEntity

@Dao
interface McpServerDao {
    @Query("SELECT * FROM mcp_servers ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<McpServerEntity>

    @Query("DELETE FROM mcp_servers")
    suspend fun clearAll()

    @Upsert()
    suspend fun insertAll(items: List<McpServerEntity>)
}
