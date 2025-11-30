package ru.izhxx.aichallenge.core.network.impl.clients.factory

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.api.factory.HttpClientModule
import ru.izhxx.aichallenge.core.network.api.factory.HttpEngineFactory

/**
 * Реализация фабрик по умолчанию.
 * EngineFactory оборачивает expect/actual createPlatformClient(config).
 * ClientFactory создает клиент и применяет модули.
 */
internal class DefaultHttpEngineFactory : HttpEngineFactory {
    override fun create(config: NetworkConfig): HttpClient = createPlatformClient(config)
}

internal class DefaultHttpClientFactory(
    private val engineFactory: HttpEngineFactory
) : HttpClientFactory {
    override fun create(
        config: NetworkConfig,
        modules: List<HttpClientModule>
    ): HttpClient {
        val engine = engineFactory.create(config)
        return engine.config {
            modules.forEach { it.install(this) }
            // Внимание: без модулей клиент будет "голым". Типичный набор модулей
            // (timeouts/json/headers/retry/ws/logging) должен передаваться потребителем.
        }
    }
}
