package ru.izhxx.aichallenge.core.logger

/**
 * Платформенный системный sink по умолчанию.
 * Реализация предоставляется в androidMain/iosMain/jvmMain.
 */
expect fun defaultPlatformSink(): LogSink
