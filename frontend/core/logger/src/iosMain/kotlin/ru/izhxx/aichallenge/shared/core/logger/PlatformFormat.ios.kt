package ru.izhxx.aichallenge.shared.core.logger

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSThread

actual fun platformNowString(): String = try {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    fmt.stringFromDate(NSDate())
} catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
    ""
}

actual fun platformThreadName(): String? = try {
    val name = NSThread.currentThread.name
    if (name != null && name.isNotEmpty()) name else null
} catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
    null
}
