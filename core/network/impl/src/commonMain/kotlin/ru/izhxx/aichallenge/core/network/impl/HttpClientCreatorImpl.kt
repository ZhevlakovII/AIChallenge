package ru.izhxx.aichallenge.core.network.impl

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import ru.izhxx.aichallenge.core.foundation.buildmode.isDebugBuild
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.core.network.impl.logger.KtorLoggerImpl

// TODO(Необходима документация)
internal class HttpClientCreatorImpl : HttpClientCreator {
    override fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
        return HttpClient {
            block()
            install(Logging) {
                level = if (isDebugBuild()) LogLevel.ALL else LogLevel.NONE
                logger = KtorLoggerImpl()
            }
        }
    }
}
