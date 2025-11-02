package ru.izhxx.aichallenge.common

/**
 * Интерфейс для логирования, который будет иметь разные реализации на разных платформах
 */
expect class Logger(tag: String) {
    /**
     * Тег логирования для идентификации источника логов
     */
    val tag: String
    
    /**
     * Логирование информационного сообщения
     */
    fun i(message: String)
    
    /**
     * Логирование отладочного сообщения
     */
    fun d(message: String)
    
    /**
     * Логирование предупреждения
     */
    fun w(message: String, throwable: Throwable? = null)
    
    /**
     * Логирование ошибки
     */
    fun e(message: String, throwable: Throwable? = null)

    companion object {
        /**
         * Создает Logger с тегом по имени класса
         */
        fun forClass(cls: Any): Logger
    }
}
