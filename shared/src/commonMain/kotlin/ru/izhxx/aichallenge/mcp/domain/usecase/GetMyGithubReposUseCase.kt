package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.domain.model.github.Repo
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * Юзкейс получения списка репозиториев аутентифицированного пользователя GitHub через MCP.
 *
 * Требует настроенного GITHUB_TOKEN на стороне MCP-сервера.
 */
class GetMyGithubReposUseCase(
    private val repository: McpRepository
) {
    /**
     * @param wsUrl WebSocket URL MCP-сервера
     * @param perPage Количество элементов (1..100)
     * @param sort created|updated|pushed|full_name
     * @param visibility all|public|private
     */
    suspend operator fun invoke(
        wsUrl: String,
        perPage: Int = 20,
        sort: String = "updated",
        visibility: String = "all"
    ): Result<List<Repo>> = repository.callListMyRepos(wsUrl, perPage, sort, visibility)
}
