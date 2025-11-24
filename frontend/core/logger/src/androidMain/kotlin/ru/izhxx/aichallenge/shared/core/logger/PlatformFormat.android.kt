package ru.izhxx.aichallenge.shared.core.logger

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun platformNowString(): String = try {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date())
} catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
    ""
}

actual fun platformThreadName(): String? = try {
    Thread.currentThread().name
} catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
    null
}
