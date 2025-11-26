package ru.izhxx.aichallenge.core.logger

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Реализация провайдера времени по умолчанию.
 * Вынесена отдельно от интерфейса TimeProvider.
 * Использует платформенную реализацию nowMillis(), которая внутри опирается на kotlinx-datetime.
 */
object DefaultTimeProvider : TimeProvider {
    @OptIn(ExperimentalTime::class)
    override fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
