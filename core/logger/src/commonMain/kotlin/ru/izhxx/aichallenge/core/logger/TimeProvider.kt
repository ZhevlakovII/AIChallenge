package ru.izhxx.aichallenge.core.logger

/**
 * Источник времени (millis since epoch).
 * Выделен в интерфейс для удобства тестирования и контроля времени.
 */
fun interface TimeProvider {
    fun now(): Long
}
