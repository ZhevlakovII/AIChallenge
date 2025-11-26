package ru.izhxx.aichallenge.core.logger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggerBehaviorTest {

    @Test
    fun `formatter is not called when level filtered out`() {
        var called = false
        val formatter = object : LogFormatter {
            override fun format(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
                timestampMillis: Long,
                threadName: String?,
                threadId: Long?,
                enrichTimestamp: Boolean,
                enrichThread: Boolean
            ): String {
                called = true
                return "SHOULD_NOT_BE_USED"
            }
        }
        val sink = CollectingSink()
        val cfg = LoggerConfig(
            minLevel = LogLevel.ERROR,
            sink = sink,
            formatter = formatter,
            timeProvider = DefaultTimeProvider,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "Tag")
        logger.info("hidden")
        assertFalse(called, "Formatter must not be called for filtered levels")
        assertTrue(sink.records.isEmpty())
    }

    @Test
    fun `time() logs finished with duration on success`() {
        val sink = CollectingSink()
        val time = SteppingTimeProvider(start = 100L, step = 10L)
        val cfg = LoggerConfig(
            minLevel = LogLevel.TRACE,
            sink = sink,
            formatter = DefaultLogFormatter,
            timeProvider = time,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "Bench")
        val result = logger.time("work", LogLevel.DEBUG) {
            // имитация полезной работы без вызовов времени
            7
        }
        assertEquals(7, result)
        assertEquals(1, sink.records.size)
        val msg = normalize(sink.records[0].message)
        assertTrue(msg.startsWith("[DEBUG] [Bench] "))
        assertTrue(msg.contains("work finished in 10ms"))
    }

    @Test
    fun `time() logs failed with duration and rethrows`() {
        val sink = CollectingSink()
        val time = SteppingTimeProvider(start = 0L, step = 5L)
        val cfg = LoggerConfig(
            minLevel = LogLevel.TRACE,
            sink = sink,
            formatter = DefaultLogFormatter,
            timeProvider = time,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "Bench")
        assertFailsWith<IllegalStateException> {
            logger.time("explode", LogLevel.DEBUG) {
                throw IllegalStateException("boom")
            }
        }
        assertEquals(1, sink.records.size)
        val rec = sink.records[0]
        assertEquals(LogLevel.ERROR, rec.level)
        val msg = normalize(rec.message)
        assertTrue(msg.startsWith("[ERROR] [Bench] "))
        assertTrue(msg.contains("explode failed in 5ms"))
        assertTrue(rec.throwable is IllegalStateException)
    }

    @Test
    fun `onSinkError is invoked when sink throws and logger does not crash`() {
        class ThrowingSink : LogSink {
            override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
                throw RuntimeException("sink error")
            }
        }
        var errorCount = 0
        val cfg = LoggerConfig(
            minLevel = LogLevel.TRACE,
            sink = ThrowingSink(),
            formatter = DefaultLogFormatter,
            timeProvider = DefaultTimeProvider,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false,
            onSinkError = { errorCount++ }
        )
        val logger = Logger({ cfg }, "Tag")
        // не должно кидать наружу
        logger.info("safe")
        assertEquals(1, errorCount)
    }

    @Test
    fun `Log global config is applied to produced Logger`() {
        val old = Log.getConfig()
        try {
            val sink = CollectingSink()
            val cfg = LoggerConfig(
                minLevel = LogLevel.INFO,
                sink = sink,
                formatter = DefaultLogFormatter,
                timeProvider = DefaultTimeProvider,
                threadInfoProvider = ConstThreadInfoProvider("thread", 1),
                enrichTimestamp = false,
                enrichThread = true
            )
            Log.setConfig(cfg)
            val logger = Log.tag("G")
            logger.info("hello")
            assertEquals(1, sink.records.size)
            val msg = normalize(sink.records[0].message)
            assertEquals("[INFO] [G] [thread:1] hello", msg)
        } finally {
            Log.setConfig(old)
        }
    }

    private fun normalize(msg: String): String {
        return if (msg.startsWith("[")) {
            val idx = msg.indexOf("] ")
            if (idx != -1 && msg.length > idx + 2 && msg.substring(1, idx).all { it.isDigit() }) {
                msg.substring(idx + 2)
            } else msg
        } else msg
    }
}
