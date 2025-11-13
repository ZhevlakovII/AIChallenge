package ru.izhxx.aichallenge.domain.model.error

/**
 * Базовое исключение для domain слоя
 * Содержит информацию об ошибке для отладки и пользовательское сообщение
 */
open class DomainException(
    val detailedMessage: String,
    cause: Throwable? = null
) : Exception(detailedMessage, cause) {
    val shortMessage = if (detailedMessage.length > 150) detailedMessage.substring(0, 150) else null
}
