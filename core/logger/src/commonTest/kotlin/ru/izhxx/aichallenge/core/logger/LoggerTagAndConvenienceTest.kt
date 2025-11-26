package ru.izhxx.aichallenge.core.logger

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LoggerTagAndConvenienceTest {

    private var saved: LoggerConfig? = null

    @AfterTest
    fun tearDown() {
        saved?.let { Log.setConfig(it) }
        saved = null
    }

    @Test
    fun `withTag overrides tag in formatted message`() {
        val sink = CollectingSink()
        val cfg = LoggerConfig(
            minLevel = LogLevel.TRACE,
            sink = sink,
            formatter = DefaultLogFormatter,
            timeProvider = DefaultTimeProvider,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "A").withTag("B")
        logger.info("hello")

        assertEquals(1, sink.records.size)
        assertEquals(LogLevel.INFO, sink.records[0].level)
        assertEquals("B", sink.records[0].tag)
        assertEquals("[INFO] [B] hello", sink.records[0].message)
    }

    @Test
    fun `Log convenience functions use global config and correct level-tag`() {
        val sink = CollectingSink()
        saved = Log.getConfig()
        Log.setConfig(
            LoggerConfig(
                minLevel = LogLevel.TRACE,
                sink = sink,
                formatter = DefaultLogFormatter,
                timeProvider = DefaultTimeProvider,
                threadInfoProvider = ConstThreadInfoProvider(null, null),
                enrichTimestamp = false,
                enrichThread = false
            )
        )

        Log.debug("T", "message")
        assertEquals(1, sink.records.size)
        assertEquals(LogLevel.DEBUG, sink.records[0].level)
        assertEquals("T", sink.records[0].tag)
        assertEquals("[DEBUG] [T] message", sink.records[0].message)
    }

    @Test
    fun `throwable passed to log is delivered to sink`() {
        val sink = CollectingSink()
        val cfg = LoggerConfig(
            minLevel = LogLevel.TRACE,
            sink = sink,
            formatter = DefaultLogFormatter,
            timeProvider = DefaultTimeProvider,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "Tag")
        val ex = IllegalArgumentException("bad")
        logger.error("fail", ex)

        assertEquals(1, sink.records.size)
        assertSame(ex, sink.records[0].throwable)
    }
}
