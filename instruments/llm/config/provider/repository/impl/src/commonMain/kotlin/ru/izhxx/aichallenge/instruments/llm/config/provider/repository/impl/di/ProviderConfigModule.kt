package ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.api.ProviderConfigRepository
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.ProviderConfigRepositoryImpl
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.dao.ProviderConfigDao

/**
 * Модуль Koin DI для [ProviderConfigRepository]
 * Провайд [ProviderConfigDao] осуществляется в корневом (app) модуле.
 */
val providerConfigModule = module {
    factory<ProviderConfigRepository> { ProviderConfigRepositoryImpl(providerConfigDao = get()) }
}