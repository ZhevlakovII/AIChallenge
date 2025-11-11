package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.features.chat.di.chatModule
import ru.izhxx.aichallenge.features.settings.di.settingsModule

/**
 * Основной модуль приложения, объединяющий все остальные модули
 */
val appModule = module {
    includes(
        listOf(
            chatModule,
            settingsModule,
            sharedModule
        )
    )
}
