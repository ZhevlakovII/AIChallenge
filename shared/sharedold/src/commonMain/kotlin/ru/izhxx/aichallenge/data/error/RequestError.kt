package ru.izhxx.aichallenge.data.error

/**
 * Ошибка выполнения запроса на клиентской стороне
 * @param stage Этап, на котором произошла ошибка
 * @param cause Исходное исключение, вызвавшее ошибку
 */
class RequestError(
    val stage: RequestStage,
    cause: Throwable
) : Exception("Ошибка на этапе ${stage.description}: ${cause.message}", cause) {
    
    /**
     * Этап, на котором произошла ошибка запроса
     */
    enum class RequestStage(val description: String) {
        SENDING("Отправка запроса"),
        PARSING("Обработка ответа")
    }
}
