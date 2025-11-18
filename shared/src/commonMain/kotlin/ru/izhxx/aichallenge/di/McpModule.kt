package ru.izhxx.aichallenge.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.izhxx.aichallenge.mcp.data.McpRepositoryImpl
import ru.izhxx.aichallenge.mcp.data.transport.McpWebSocketClient
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpToolsUseCase

/**
 * Shared Koin-модуль для MCP: транспорт, репозиторий, use case.
 * ViewModel НЕ регистрируется здесь (она живёт в composeApp).
 */
val mcpSharedModule: Module = module {
    // Transport
    single { McpWebSocketClient(httpClient = get(), json = get()) }

    // Repository
    singleOf(::McpRepositoryImpl) bind McpRepository::class

    // UseCase
    factoryOf(::GetMcpToolsUseCase)
}
