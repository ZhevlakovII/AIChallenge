package ru.izhxx.aichallenge.rag.docindexer.core.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharOverlapChunkerTest {

    @Test
    fun testSingleRangeWhenShorterThanMax() {
        val text = "Short text"
        val maxChars = 100
        val overlap = 20

        val chunker = CharOverlapChunker()
        val ranges = chunker.split(text, maxChars, overlap)

        // Должен быть один диапазон, покрывающий весь текст
        assertEquals(1, ranges.size, "Expected single chunk for short text")
        val r = ranges.first()
        assertEquals(0, r.first, "Chunk must start at 0")
        assertEquals(text.length, r.last + 1, "Chunk must endExclusive == text.length")
        assertTrue((r.last + 1) - r.first <= maxChars, "Chunk length must be <= maxChars")
    }

    @Test
    fun testSlidingWindowWithOverlapNoHeuristics() {
        // Без переносов строки, чтобы хевристика не срабатывала
        val textLen = 1000
        val text = "a".repeat(textLen)
        val maxChars = 100
        val overlap = 20

        val chunker = CharOverlapChunker()
        val ranges = chunker.split(text, maxChars, overlap)

        assertTrue(ranges.isNotEmpty(), "Chunks must not be empty")

        // Проверяем длины чанков и прогресс
        for (i in ranges.indices) {
            val r = ranges[i]
            val endExclusive = r.last + 1
            val len = endExclusive - r.first
            assertTrue(len in 1..maxChars, "Each chunk length must be 1..maxChars")

            if (i > 0) {
                val prev = ranges[i - 1]
                val prevEndExclusive = prev.last + 1
                // следующий старт должен совпадать с prevEnd - overlap
                val expectedStart = (prevEndExclusive - overlap).coerceAtLeast(0)
                assertEquals(expectedStart, r.first, "Next chunk must start at prevEndExclusive - overlap")
                assertTrue(r.first <= prevEndExclusive, "Chunks must overlap or touch")
            }
        }

        // Последний чанк должен доходить до конца текста
        val last = ranges.last()
        assertEquals(textLen, last.last + 1, "Last chunk endExclusive must equal text length")
    }

    @Test
    fun testHeuristicCutsOnDoubleNewline() {
        // Вставим двойной перенос ближе к targetEnd, чтобы сработал сдвиг назад
        val a = "A".repeat(150)      // 0..149
        val cutAt = a.length         // 150
        val text = a + "\n\n" + "B".repeat(150)
        val maxChars = 180           // targetEnd ~ 180
        val overlap = 30

        val chunker = CharOverlapChunker()
        val ranges = chunker.split(text, maxChars, overlap)

        assertTrue(ranges.isNotEmpty(), "Chunks must not be empty")
        val first = ranges.first()
        val endExclusive = first.last + 1

        // Ожидаем, что хевристика отрежет по началу "\n\n", то есть по индексу 150
        assertEquals(cutAt, endExclusive, "Heuristic should snap end to the nearest double newline boundary")
        assertTrue(endExclusive <= maxChars, "Heuristic boundary must not exceed maxChars when available behind target")
    }
}
