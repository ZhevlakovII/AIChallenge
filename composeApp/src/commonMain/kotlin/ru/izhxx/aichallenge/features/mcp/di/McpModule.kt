package ru.izhxx.aichallenge.features.mcp.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.mcp.presentation.McpViewModel

/**
 * Koin-модуль фичи MCP в composeApp:
 * Регистрирует только ViewModel. Остальные зависимости находятся в shared (mcpSharedModule).
 */
val mcpModule: Module = module {
    viewModel {
        McpViewModel(
            getMcpTools = get(),
            ensureConnected = get(),
            getServers = get(),
            saveServers = get()
        )
    }
}
