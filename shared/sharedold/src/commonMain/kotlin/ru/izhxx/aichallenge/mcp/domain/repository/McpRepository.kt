package ru.izhxx.aichallenge.mcp.domain.repository

import ru.izhxx.aichallenge.domain.model.github.Repo
import ru.izhxx.aichallenge.mcp.domain.model.McpTool
import kotlinx.serialization.json.JsonElement

/**
 * Репозиторий для взаимодействия с MCP-сервером.
 */
interface McpRepository {
    /**
     * Устанавливает MCP-соединение и возвращает список доступных инструментов.
     *
     * @param wsUrl WebSocket URL MCP сервера (например, ws://localhost:3000/mcp)
     */
    suspend fun listTools(wsUrl: String): Result<List<McpTool>>

    /**
     * Универсальный вызов MCP-инструмента по имени.
     *
     * @param wsUrl WebSocket URL MCP сервера
     * @param name имя инструмента
     * @param arguments аргументы инструмента в формате JSON
     */
    suspend fun callTool(
        wsUrl: String,
        name: String,
        arguments: JsonElement
    ): Result<JsonElement>

    /**
     * Вызывает инструмент github.list_user_repos на MCP-сервере.
     *
     * @param wsUrl WebSocket URL MCP сервера
     * @param username Имя пользователя GitHub
     * @param perPage Количество элементов на страницу (1..100)
     * @param sort Поле сортировки (created|updated|pushed|full_name)
     */
    suspend fun callListUserRepos(
        wsUrl: String,
        username: String,
        perPage: Int = 20,
        sort: String = "updated"
    ): Result<List<Repo>>

    /**
     * Вызывает инструмент github.list_my_repos на MCP-сервере.
     *
     * Требует настройки токена на стороне сервера (GITHUB_TOKEN).
     *
     * @param wsUrl WebSocket URL MCP сервера
     * @param perPage Количество элементов на страницу (1..100)
     * @param sort Поле сортировки (created|updated|pushed|full_name)
     * @param visibility Фильтр видимости (all|public|private)
     */
    suspend fun callListMyRepos(
        wsUrl: String,
        perPage: Int = 20,
        sort: String = "updated",
        visibility: String = "all"
    ): Result<List<Repo>>
}
