package ru.izhxx.aichallenge.data.error

/**
 * Ошибка, возвращенная API сервером
 * @param statusCode HTTP-код ответа
 * @param errorBody Тело ответа с описанием ошибки
 */
class ApiError(
    val statusCode: Int,
    val errorBody: String?
) : Exception(generateMessage(statusCode, errorBody)) {

    companion object {
        private fun generateMessage(statusCode: Int, errorBody: String?): String {
            return StringBuilder().apply {
                append(
                    when (statusCode) {
                        401 -> "Ошибка аутентификации (API ключ неверный)"
                        403 -> "Доступ запрещён"
                        429 -> "Лимит запросов превышен"
                        500 -> "Ошибка сервера"
                        502 -> "Ошибка шлюза"
                        503 -> "Сервис недоступен"
                        else -> "Ошибка API (код $statusCode)"
                    }
                )
                append(": ")
                errorBody?.let {
                    append(it)
                }
            }.toString()
        }
    }
}
