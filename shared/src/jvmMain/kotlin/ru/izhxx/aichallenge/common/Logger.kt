package ru.izhxx.aichallenge.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * JVM-реализация Logger, использует System.out для вывода логов
 */
actual class Logger actual constructor(actual val tag: String) {
    
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    
    actual fun i(message: String) {
        println("${formatTimestamp()} INFO [$tag] $message")
    }
    
    actual fun d(message: String) {
        println("${formatTimestamp()} DEBUG [$tag] $message")
    }
    
    actual fun w(message: String, throwable: Throwable?) {
        println("${formatTimestamp()} WARN [$tag] $message")
        throwable?.printStackTrace()
    }
    
    actual fun e(message: String, throwable: Throwable?) {
        System.err.println("${formatTimestamp()} ERROR [$tag] $message")
        throwable?.printStackTrace()
    }
    
    private fun formatTimestamp(): String {
        return LocalDateTime.now().format(dateTimeFormatter)
    }
    
    actual companion object {
        actual fun forClass(cls: Any): Logger {
            return Logger(cls::class.simpleName ?: "UnknownClass")
        }
    }
}
