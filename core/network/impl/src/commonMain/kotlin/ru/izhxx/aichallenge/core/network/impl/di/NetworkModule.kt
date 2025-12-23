package ru.izhxx.aichallenge.core.network.impl.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.core.network.impl.HttpClientCreatorImpl

/**
 * Модуль Koin для сетевых зависимостей (HTTP клиент и JSON сериализация).
 *
 * Назначение:
 * - Регистрирует singleton реализацию [HttpClientCreator] для создания HTTP клиентов.
 * - Регистрирует singleton экземпляр [Json] для сериализации/десериализации данных.
 *
 * Регистрируемые зависимости:
 * - [HttpClientCreator] — используется для создания Ktor HttpClient с автоматической конфигурацией.
 * - [Json] — настроен для работы с JSON (ignoreUnknownKeys, isLenient).
 *
 * Конфигурация Json:
 * - `ignoreUnknownKeys = true` — игнорирует неизвестные поля в JSON (защита от изменений API).
 * - `isLenient = true` — позволяет парсить некорректно отформатированный JSON.
 *
 * Использование:
 * ```kotlin
 * // В настройке Koin:
 * startKoin {
 *     modules(networkModule, ...)
 * }
 *
 * // В классе:
 * class ApiService(
 *     private val httpClientCreator: HttpClientCreator,
 *     private val json: Json
 * ) {
 *     private val client = httpClientCreator.buildHttpClient { ... }
 * }
 * ```
 *
 * @see HttpClientCreator
 * @see HttpClientCreatorImpl
 * @see Json
 */
val networkModule = module {
    single<HttpClientCreator> { HttpClientCreatorImpl() }
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
