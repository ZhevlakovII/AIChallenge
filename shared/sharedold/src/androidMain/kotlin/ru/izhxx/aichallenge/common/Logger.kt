package ru.izhxx.aichallenge.common

import android.util.Log

/**
 * Android-реализация Logger, использует Log из Android SDK
 */
actual class Logger actual constructor(actual val tag: String) {
    
    actual fun i(message: String) {
        Log.i(tag, message)
    }
    
    actual fun d(message: String) {
        Log.d(tag, message)
    }
    
    actual fun w(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    actual fun e(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    actual companion object {
        actual fun forClass(cls: Any): Logger {
            return Logger(cls::class.java.simpleName)
        }
    }
}
