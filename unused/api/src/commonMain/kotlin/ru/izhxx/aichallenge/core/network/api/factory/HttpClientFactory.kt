package ru.izhxx.aichallenge.core.network.api.factory

import io.ktor.client.HttpClient
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

/**
 * Фабрика итогового HttpClient: создает клиент на базе HttpEngineFactory и применяет список модулей.
 */
interface HttpClientFactory {
    fun create(config: NetworkConfig, modules: List<HttpClientModule> = emptyList()): HttpClient
}
