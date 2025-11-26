package ru.izhxx.aichallenge.core.logger

/**
 * Поставщик информации о текущем "потоке выполнения".
 * На разных платформах может означать реальный системный поток или аналог (корутина ≠ поток).
 */
interface ThreadInfoProvider {
    fun currentName(): String?
    fun currentId(): Long?
}
