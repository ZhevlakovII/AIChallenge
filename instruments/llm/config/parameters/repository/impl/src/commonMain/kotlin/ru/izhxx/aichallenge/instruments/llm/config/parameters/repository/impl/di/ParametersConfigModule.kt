package ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.api.ParametersConfigRepository
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.ParametersConfigRepositoryImpl
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.dao.ParametersConfigDao

/**
 * Модуль Koin DI для [ParametersConfigRepository]
 * Провайд [ParametersConfigDao] осуществляется в корневом (app) модуле.
 */
val parametersConfigModule = module {
    factory<ParametersConfigRepository> { ParametersConfigRepositoryImpl(parametersConfigDao = get()) }
}