package ru.izhxx.aichallenge.core.logger

/**
 * Платформенно-специфичное текущее время в миллисекундах с epoch.
 * Разделяем через expect/actual, чтобы не тащить дополнительные зависимости и избежать несовместимости stdlib.
 */
expect fun nowMillis(): Long
