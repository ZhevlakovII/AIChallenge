package ru.izhxx.aichallenge.core.network.impl.clients.factory

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

internal actual fun createPlatformClient(config: NetworkConfig): HttpClient {
    // Движок OkHttp для Android. Плагины и таймауты навешиваются в common-конфигурации.
    return HttpClient(OkHttp) {
        engine {
            // Здесь можно добавить OkHttp-specific настройки при необходимости (pinning и т.п.)
        }
    }
}
