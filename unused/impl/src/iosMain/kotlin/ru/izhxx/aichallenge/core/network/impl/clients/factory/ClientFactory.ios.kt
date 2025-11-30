package ru.izhxx.aichallenge.core.network.impl.clients.factory

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

internal actual fun createPlatformClient(config: NetworkConfig): HttpClient {
    // Движок Darwin для iOS. Общие плагины ставятся в common-конфигурации.
    return HttpClient(Darwin) {
        engine {
            // Доп. настройки при необходимости (например, конфиг сессии)
        }
    }
}
