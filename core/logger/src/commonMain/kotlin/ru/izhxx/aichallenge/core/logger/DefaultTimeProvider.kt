package ru.izhxx.aichallenge.core.logger

/**
 * Реализация провайдера времени по умолчанию.
 * Вынесена отдельно от интерфейса TimeProvider.
 * Использует платформенную реализацию nowMillis(), которая внутри опирается на kotlinx-datetime.
 */
object DefaultTimeProvider : TimeProvider {
    override fun now(): Long = nowMillis()
}
