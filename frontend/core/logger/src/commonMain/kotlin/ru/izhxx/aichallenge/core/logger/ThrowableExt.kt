package ru.izhxx.aichallenge.core.logger

/**
 * Платформенно-специфичное получение строки для Throwable.
 */
expect fun Throwable.asString(): String
