package ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.tools.llm.config.storage.api.parameters.ParametersConfigRepository
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.ParametersConfigRepositoryImpl
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.dao.ParametersConfigDao

/**
 * Модуль Koin DI для [ParametersConfigRepository]
 * Провайд [ParametersConfigDao] осуществляется в корневом (app) модуле.
 */
val parametersConfigModule = module {
    factory<ParametersConfigRepository> { ParametersConfigRepositoryImpl(parametersConfigDao = get()) }
}