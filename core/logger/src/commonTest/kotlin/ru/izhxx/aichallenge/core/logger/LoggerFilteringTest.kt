package ru.izhxx.aichallenge.core.logger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerFilteringTest {

    @Test
    fun `levels below minLevel are filtered out`() {
        val sink = CollectingSink()
        val cfg = LoggerConfig(
            minLevel = LogLevel.WARN,
            sink = sink,
            formatter = DefaultLogFormatter,
            timeProvider = DefaultTimeProvider,
            threadInfoProvider = ConstThreadInfoProvider("t", 1),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "Tag")

        // ниже WARN — не должны пройти
        logger.trace("t1")
        logger.debug("d1")
        logger.info("i1")
        // от WARN и выше — проходят
        logger.warn("w1")
        logger.error("e1")
        logger.wtf("a1")

        val msgs = sink.records.map { it.message }
        assertEquals(listOf("[WARN] [Tag] w1", "[ERROR] [Tag] e1", "[ASSERT] [Tag] a1"), msgs.map { normalize(it) })
        assertTrue(sink.records.all { it.level.ordinal >= LogLevel.WARN.ordinal })
    }

    @Test
    fun `OFF disables all levels`() {
        val sink = CollectingSink()
        val cfg = LoggerConfig(
            minLevel = LogLevel.OFF,
            sink = sink,
            formatter = DefaultLogFormatter,
            timeProvider = DefaultTimeProvider,
            threadInfoProvider = ConstThreadInfoProvider(null, null),
            enrichTimestamp = false,
            enrichThread = false
        )
        val logger = Logger({ cfg }, "OffTag")

        logger.trace("a"); logger.debug("b"); logger.info("c")
        logger.warn("d"); logger.error("e"); logger.wtf("f")

        assertEquals(0, sink.records.size)
    }

    // Помощник: убираем префиксы с временем, чтобы сравнивать независимо от timestamp
    private fun normalize(msg: String): String {
        // Срезаем префикс только если это действительно timestamp (цифры внутри первых скобок)
        return if (msg.startsWith("[")) {
            val idx = msg.indexOf("] ")
            if (idx != -1 && msg.length > idx + 2 && msg.substring(1, idx).all { it.isDigit() }) {
                msg.substring(idx + 2)
            } else msg
        } else {
            msg
        }
    }
}
