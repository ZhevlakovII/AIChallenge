package ru.izhxx.aichallenge.core.foundation.error

import kotlin.jvm.JvmInline

/**
 * Класс-обёртка для ключа метадаты
 */
@JvmInline
value class MetadataKey internal constructor(val key: String)