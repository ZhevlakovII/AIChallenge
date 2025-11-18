package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.domain.model.github.Repo
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * Юзкейс получения списка публичных репозиториев пользователя GitHub через MCP.
 */
class GetGithubUserReposUseCase(
    private val repository: McpRepository
) {
    /**
     * @param wsUrl WebSocket URL MCP-сервера
     * @param username GitHub username
     * @param perPage Количество элементов (1..100)
     * @param sort created|updated|pushed|full_name
     */
    suspend operator fun invoke(
        wsUrl: String,
        username: String,
        perPage: Int = 20,
        sort: String = "updated"
    ): Result<List<Repo>> = repository.callListUserRepos(wsUrl, username, perPage, sort)
}
