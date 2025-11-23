package ru.izhxx.aichallenge.core.logger

/**
 * Удобные фабрики для создания логгеров.
 */
fun logger(tag: String): Logger = Logger(tag)

fun loggerForClass(owner: Any): Logger = Logger.forClass(owner)

/**
 * Reified-вариант: Logger по имени типа T.
 */
inline fun <reified T> logger(): Logger = Logger(T::class.simpleName ?: "Unknown")
