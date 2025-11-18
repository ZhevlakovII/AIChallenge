package ru.izhxx.aichallenge.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.izhxx.aichallenge.data.repository.McpConfigRepositoryImpl
import ru.izhxx.aichallenge.domain.repository.McpConfigRepository
import ru.izhxx.aichallenge.mcp.data.McpRepositoryImpl
import ru.izhxx.aichallenge.mcp.data.transport.McpWebSocketClient
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository
import ru.izhxx.aichallenge.mcp.domain.usecase.EnsureMcpConnectedUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetGithubUserReposUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpToolsUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMyGithubReposUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetSavedMcpUrlUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.SaveMcpUrlUseCase

/**
 * Shared Koin-модуль для MCP: транспорт, репозиторий, use case.
 * ViewModel НЕ регистрируется здесь (она живёт в composeApp).
 */
val mcpSharedModule: Module = module {
    // Transport
    single { McpWebSocketClient(httpClient = get(), json = get()) }

    // Config repository (DataStore-backed)
    singleOf(::McpConfigRepositoryImpl) bind McpConfigRepository::class

    // Repository
    singleOf(::McpRepositoryImpl) bind McpRepository::class

    // UseCases
    factoryOf(::GetMcpToolsUseCase)
    factoryOf(::EnsureMcpConnectedUseCase)
    factoryOf(::GetSavedMcpUrlUseCase)
    factoryOf(::SaveMcpUrlUseCase)
    factoryOf(::GetGithubUserReposUseCase)
    factoryOf(::GetMyGithubReposUseCase)
}
