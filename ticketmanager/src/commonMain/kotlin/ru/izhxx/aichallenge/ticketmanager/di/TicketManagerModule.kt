package ru.izhxx.aichallenge.ticketmanager.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.di.sharedModule
import ru.izhxx.aichallenge.features.productassistant.impl.di.productAssistantModule

/**
 * Главный DI модуль для Ticket Manager приложения
 */
val ticketManagerModule = module {
    // Включаем модуль Product Assistant для работы с тикетами
    includes(
        productAssistantModule,
        sharedModule,

    )
}
