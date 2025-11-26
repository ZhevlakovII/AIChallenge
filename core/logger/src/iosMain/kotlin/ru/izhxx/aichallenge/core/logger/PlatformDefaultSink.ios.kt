package ru.izhxx.aichallenge.core.logger

import platform.Foundation.NSLog

actual fun defaultPlatformSink(): LogSink = LogSink { level, _ /* tag уже включён форматтером */, message, throwable ->
    if (throwable != null) {
        NSLog("%@", "$message\n$throwable")
    } else {
        NSLog("%@", message)
    }
}
