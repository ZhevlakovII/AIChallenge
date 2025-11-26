package ru.izhxx.aichallenge.core.logger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultLogFormatterTest {

    @Test
    fun `no enrichments - only level and tag prefix`() {
        val msg = DefaultLogFormatter.format(
            level = LogLevel.INFO,
            tag = "T",
            message = "hello",
            throwable = null,
            timestampMillis = 123L,
            threadName = null,
            threadId = null,
            enrichTimestamp = false,
            enrichThread = false
        )
        assertEquals("[INFO] [T] hello", msg)
    }

    @Test
    fun `with timestamp - millis enclosed in brackets`() {
        val msg = DefaultLogFormatter.format(
            level = LogLevel.DEBUG,
            tag = "T",
            message = "m",
            throwable = null,
            timestampMillis = 9999L,
            threadName = null,
            threadId = null,
            enrichTimestamp = true,
            enrichThread = false
        )
        // начало строки должно содержать [9999]
        assertTrue(msg.startsWith("[9999] "))
        assertTrue(msg.contains("[DEBUG] [T] m"))
    }

    @Test
    fun `with thread name and id`() {
        val msg = DefaultLogFormatter.format(
            level = LogLevel.WARN,
            tag = "Core",
            message = "msg",
            throwable = null,
            timestampMillis = 0L,
            threadName = "main",
            threadId = 1L,
            enrichTimestamp = false,
            enrichThread = true
        )
        assertEquals("[WARN] [Core] [main:1] msg", msg)
    }

    @Test
    fun `with thread only name`() {
        val msg = DefaultLogFormatter.format(
            level = LogLevel.ERROR,
            tag = "Core",
            message = "x",
            throwable = null,
            timestampMillis = 0L,
            threadName = "io",
            threadId = null,
            enrichTimestamp = false,
            enrichThread = true
        )
        assertEquals("[ERROR] [Core] [io] x", msg)
    }

    @Test
    fun `with thread only id`() {
        val msg = DefaultLogFormatter.format(
            level = LogLevel.ERROR,
            tag = "Core",
            message = "x",
            throwable = null,
            timestampMillis = 0L,
            threadName = null,
            threadId = 42L,
            enrichTimestamp = false,
            enrichThread = true
        )
        assertEquals("[ERROR] [Core] [42] x", msg)
    }

    @Test
    fun `throwable is hinted in message`() {
        val ex = IllegalStateException("boom")
        val msg = DefaultLogFormatter.format(
            level = LogLevel.ERROR,
            tag = "Tag",
            message = "m",
            throwable = ex,
            timestampMillis = 0L,
            threadName = null,
            threadId = null,
            enrichTimestamp = false,
            enrichThread = false
        )
        assertTrue(msg.startsWith("[ERROR] [Tag] m"))
        assertTrue(msg.contains(" | ex=${ex}"))
    }
}
