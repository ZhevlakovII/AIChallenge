package ru.izhxx.aichallenge.core.logger

import kotlinx.datetime.Clock

actual fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
