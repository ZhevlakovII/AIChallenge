package ru.izhxx.aichallenge.rag.docindexer.core.impl

import ru.izhxx.aichallenge.rag.docindexer.core.api.TextChunker
import kotlin.math.max
import kotlin.math.min

/**
 * Простой чанкер по символам со скользящим окном и overlap.
 * Пытается подвинуть границу к ближайшей "естественной" границе (двойной перенос строки или заголовок Markdown)
 * в радиусе heuristicsRadius от целевого cut.
 *
 * ВОЗВРАЩАЕМЫЕ ДИАПАЗОНЫ: интерпретируются как [start, end), но с использованием IntRange.
 * Поэтому в IntRange сохраняется [start, endExclusive-1]. Для получения текста используйте:
 *   text.substring(range.first, range.last + 1)
 */
class CharOverlapChunker(
    private val heuristicsRadius: Int = 200
) : TextChunker {

    override fun split(text: String, maxChars: Int, overlapChars: Int): List<IntRange> {
        if (text.isBlank()) return emptyList()
        require(maxChars > 0) { "maxChars must be > 0" }
        require(overlapChars in 0..<maxChars) { "overlapChars must be >= 0 and < maxChars" }

        val ranges = mutableListOf<IntRange>()
        var start = 0
        val n = text.length
        println("      [Chunker] Text length: $n chars, maxChars: $maxChars, overlap: $overlapChars")

        var iterations = 0
        val maxIterations = n + 100 // защита от бесконечного цикла

        while (start < n) {
            iterations++
            if (iterations > maxIterations) {
                println("      [Chunker] ❌ ERROR: Too many iterations ($iterations), breaking to prevent infinite loop")
                println("      [Chunker]   Current state: start=$start, n=$n, ranges=${ranges.size}")
                break
            }

            val targetEnd = min(start + maxChars, n)
            val endExclusive = chooseBoundary(text, start, targetEnd, n)
            val endExclusiveFixed = max(start + 1, endExclusive) // гарантируем прогресс хотя бы на 1 символ

            // IntRange — включительно, поэтому сохраняем [start, endExclusive-1]
            ranges.add(start..<endExclusiveFixed)

            if (endExclusiveFixed >= n) break

            // следующий старт с overlap
            val nextStart = max(0, endExclusiveFixed - overlapChars)

            // CRITICAL: гарантируем прогресс - новый start должен быть строго больше старого
            // Иначе получим бесконечный цикл, если overlapChars >= (endExclusiveFixed - start)
            if (nextStart <= start) {
                // Если overlap слишком большой, двигаемся хотя бы на 1 символ вперёд
                println("      [Chunker] ⚠️  Overlap too large, forcing progress: start $start → ${start + 1}")
                start = start + 1
            } else {
                start = nextStart
            }

            if (start >= n) break
        }

        println("      [Chunker] Split complete: ${ranges.size} chunks in $iterations iterations")
        return ranges
    }

    private fun chooseBoundary(text: String, start: Int, targetEnd: Int, textLength: Int): Int {
        val left = max(start, targetEnd - heuristicsRadius)
        val right = min(textLength, targetEnd + heuristicsRadius)

        // 1) Ищем сдвиг назад до последней "пустой строки" (\n\n)
        val backBoundByBlank = lastIndexOf(text, "\n\n", startIndex = left, endIndexExclusive = right)
        // 2) Ищем сдвиг назад до заголовка (\n#)
        val backBoundByHeader = lastIndexOf(text, "\n#", startIndex = left, endIndexExclusive = right)

        val candidatesBack = listOf(backBoundByBlank, backBoundByHeader).filter { it != -1 && it > start }
        if (candidatesBack.isNotEmpty()) {
            val best = candidatesBack.max() // наиболее правый кандидат
            return min(best, textLength)
        }

        // 3) Если назад не нашли — пробуем вперёд к ближайшей пустой строке или заголовку
        val forwardBlank = indexOf(text, "\n\n", startIndex = targetEnd, endIndexExclusive = right)
        val forwardHeader = indexOf(text, "\n#", startIndex = targetEnd, endIndexExclusive = right)

        val candidatesForward = listOf(forwardBlank, forwardHeader).filter { it != -1 }
        if (candidatesForward.isNotEmpty()) {
            val best = candidatesForward.min() // ближайший вперёд
            return min(max(best, start + 1), textLength)
        }

        // 4) Иначе — режем по целевому
        return min(targetEnd, textLength)
    }

    private fun lastIndexOf(text: String, needle: String, startIndex: Int, endIndexExclusive: Int): Int {
        if (startIndex >= endIndexExclusive) return -1
        val sub = text.substring(startIndex, endIndexExclusive)
        val idx = sub.lastIndexOf(needle)
        return if (idx == -1) -1 else (startIndex + idx)
    }

    private fun indexOf(text: String, needle: String, startIndex: Int, endIndexExclusive: Int): Int {
        if (startIndex >= endIndexExclusive) return -1
        val sub = text.substring(startIndex, endIndexExclusive)
        val idx = sub.indexOf(needle)
        return if (idx == -1) -1 else (startIndex + idx)
    }
}
