package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.features.chat.di.chatModule
import ru.izhxx.aichallenge.features.mcp.di.mcpModule
import ru.izhxx.aichallenge.features.pranalyzer.impl.di.prAnalyzerModule
import ru.izhxx.aichallenge.features.productassistant.impl.di.productAssistantModule
import ru.izhxx.aichallenge.features.reminder.di.reminderModule
import ru.izhxx.aichallenge.features.settings.di.settingsModule

/**
 * Основной модуль приложения, объединяющий все остальные модули
 */
val appModule = module {
    includes(
        listOf(
            chatModule,
            settingsModule,
            mcpModule,
            reminderModule,
            prAnalyzerModule,
            productAssistantModule,
            sharedModule
        )
    )
}
