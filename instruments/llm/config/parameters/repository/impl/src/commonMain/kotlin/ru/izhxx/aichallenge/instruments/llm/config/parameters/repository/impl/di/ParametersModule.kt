package ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.api.ParametersConfigRepository
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.ParametersConfigRepositoryImpl

val parametersModule = module {
    factory<ParametersConfigRepository> { ParametersConfigRepositoryImpl(parametersConfigDao = get()) }
}