package ru.izhxx.aichallenge.core.network.impl.logger

import io.ktor.client.plugins.logging.Logger
import ru.izhxx.aichallenge.core.foundation.logger.Tag
import ru.izhxx.aichallenge.core.foundation.logger.debug

internal class KtorLoggerImpl : Logger {

    private val loggerTag = Tag.of("KtorLoggerImpl")

    override fun log(message: String) {
        debug(loggerTag) { message }
    }
}
