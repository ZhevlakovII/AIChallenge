package ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.tools.llm.config.storage.api.provider.ProviderConfigRepository
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.ProviderConfigRepositoryImpl
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.dao.ProviderConfigDao

/**
 * Модуль Koin DI для [ProviderConfigRepository]
 * Провайд [ProviderConfigDao] осуществляется в корневом (app) модуле.
 */
val providerConfigModule = module {
    factory<ProviderConfigRepository> { ProviderConfigRepositoryImpl(providerConfigDao = get()) }
}