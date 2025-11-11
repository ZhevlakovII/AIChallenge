package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.features.chat.di.chatModule
import ru.izhxx.aichallenge.features.settings.di.settingsModule

/**
 * Основной модуль приложения, объединяющий все остальные модули
 */
val appModule = module {
    // Включаем viewModelModule из текущего пакета
    includes(chatModule, settingsModule)

    // Включаем модули из shared
    // apiModule и dataModule будут доступны через Koin
    includes(sharedModule)
}
