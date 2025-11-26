package ru.izhxx.aichallenge.core.logger

/**
 * Платформенная реализация ThreadInfoProvider по умолчанию.
 * Интерфейс отделён (ThreadInfoProvider), реализация предоставляется через expect/actual.
 */
expect object DefaultThreadInfoProvider : ThreadInfoProvider
