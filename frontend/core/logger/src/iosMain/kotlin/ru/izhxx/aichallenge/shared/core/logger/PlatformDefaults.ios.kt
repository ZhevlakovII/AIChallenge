package ru.izhxx.aichallenge.shared.core.logger

import ru.izhxx.aichallenge.core.logger.LoggerConfig

actual object PlatformDefaults {
    actual fun installDefaultSinks() {
        // iOS: вывод через NSLog
        LoggerConfig.addSink(NSLogSink())
    }
}
