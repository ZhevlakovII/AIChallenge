package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.data.repository.MetricsCacheRepositoryImpl
import ru.izhxx.aichallenge.domain.repository.MetricsCacheRepository

/**
 * Модуль для DI компонентов, связанных с метриками
 */
val metricsModule = module {
    // Регистрируем репозиторий метрик как синглтон
    single<MetricsCacheRepository> { MetricsCacheRepositoryImpl() }
}
