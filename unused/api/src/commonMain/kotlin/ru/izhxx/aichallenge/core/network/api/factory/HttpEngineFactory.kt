package ru.izhxx.aichallenge.core.network.api.factory

import io.ktor.client.HttpClient
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

/**
 * Фабрика платформенного HttpClient (движок + минимальные настройки уровня engine).
 * Плагины/интерсепторы настраиваются отдельными модулями через HttpClientModule.
 */
interface HttpEngineFactory {
    fun create(config: NetworkConfig): HttpClient
}
