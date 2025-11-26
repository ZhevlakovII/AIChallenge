package ru.izhxx.aichallenge.core.logger

/**
 * Уровни логирования в порядке возрастания важности.
 * OFF — полностью отключает логирование.
 */
enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    ASSERT,
    OFF
}
