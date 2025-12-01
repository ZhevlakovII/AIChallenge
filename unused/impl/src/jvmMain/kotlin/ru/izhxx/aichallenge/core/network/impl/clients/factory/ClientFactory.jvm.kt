package ru.izhxx.aichallenge.core.network.impl.clients.factory

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

internal actual fun createPlatformClient(config: NetworkConfig): HttpClient {
    // Движок CIO для JVM/desktop. Плагины и таймауты навешиваются в common-конфигурации.
    return HttpClient(CIO) {
        engine {
            // Можно настроить пул коннектов/прокси и т.п. при необходимости
        }
    }
}
