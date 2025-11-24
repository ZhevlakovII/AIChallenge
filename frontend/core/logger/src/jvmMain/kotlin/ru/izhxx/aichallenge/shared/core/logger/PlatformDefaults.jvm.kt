package ru.izhxx.aichallenge.shared.core.logger

import ru.izhxx.aichallenge.core.logger.ConsoleSink
import ru.izhxx.aichallenge.core.logger.LoggerConfig

actual object PlatformDefaults {
    actual fun installDefaultSinks() {
        // На JVM предпочитаем SLF4J. Если подключен только NOPLoggerFactory — используем ConsoleSink.
        val factory = org.slf4j.LoggerFactory.getILoggerFactory()
        val factoryName = factory.javaClass.name
        val nopName = org.slf4j.helpers.NOPLoggerFactory::class.java.name
        val isNOP = factoryName == nopName

        if (isNOP) {
            LoggerConfig.addSink(ConsoleSink())
        } else {
            LoggerConfig.addSink(Slf4jSink())
        }
    }
}
