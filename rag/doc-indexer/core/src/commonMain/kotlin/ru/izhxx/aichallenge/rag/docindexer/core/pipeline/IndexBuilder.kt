package ru.izhxx.aichallenge.rag.docindexer.core.pipeline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import ru.izhxx.aichallenge.rag.docindexer.core.api.ContentReader
import ru.izhxx.aichallenge.rag.docindexer.core.api.Embedder
import ru.izhxx.aichallenge.rag.docindexer.core.api.Hasher
import ru.izhxx.aichallenge.rag.docindexer.core.api.TextChunker
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildParams
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildRequest
import ru.izhxx.aichallenge.rag.docindexer.core.model.Chunk
import ru.izhxx.aichallenge.rag.docindexer.core.model.Document
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import ru.izhxx.aichallenge.rag.docindexer.core.model.IndexStats
import ru.izhxx.aichallenge.rag.docindexer.core.model.ModelConfig
import ru.izhxx.aichallenge.rag.docindexer.core.model.SourceConfig
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class IndexBuilder(
    private val reader: ContentReader,
    private val chunker: TextChunker,
    private val embedder: Embedder,
    private val hasher: Hasher
) {
    /**
     * Основная сборка индекса.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun build(request: BuildRequest): DocumentIndex {
        val startNs = Clock.System.now().nanosecondsOfSecond

        val inputDir = request.inputDir
        val params = request.params
        val model = request.model

        val semaphore = Semaphore(params.concurrency)

        val docs = mutableListOf<Document>()
        var totalChunks = 0
        var totalChunkLen = 0

        // Scan for .md files only
        val files = reader.scanMarkdownFiles(inputDir)
        println("📁 Found files: ${files.size}")

        coroutineScope {
            files.forEachIndexed { fileIndex, entry ->
                println("📄 [${fileIndex + 1}/${files.size}] Processing: ${entry.relativePath}")

                // Process file in separate block to release intermediate data
                val doc = run {
                    val content = normalize(reader.read(entry))
                    val contentLength = content.length
                    println("   📏 Size: ${contentLength / 1024} KB")

                    val ranges = chunker.split(content, params.maxChars, params.overlapChars)
                    println("   ✂️  Created chunks: ${ranges.size}")

                    // Generate embeddings for chunks - parallel but rate-limited
                    println("   🔄 Generating embeddings (concurrency=${params.concurrency})...")
                    val embeddings = try {
                        ranges.mapIndexed { idx, range ->
                            val text = safeSubstring(content, range)
                            async(Dispatchers.Default) {
                                semaphore.withPermit {
                                    println("      → Chunk ${idx + 1}/${ranges.size}: ${text.take(50).replace("\n", " ")}...")
                                    val embedding = embedder.embed(text)
                                    println("      ✓ Chunk ${idx + 1}/${ranges.size} embedded (dim=${embedding.size})")
                                    embedding
                                }
                            }
                        }.awaitAll()
                    } catch (e: Exception) {
                        println("      ❌ ERROR generating embeddings: ${e.message}")
                        throw e
                    }
                    println("   ✅ All embeddings generated")

                    val chunks = ranges.mapIndexed { idx, range ->
                        val text = safeSubstring(content, range)
                        totalChunks += 1
                        totalChunkLen += text.length
                        Chunk(
                            id = "${entry.relativePath}::$idx",
                            index = idx,
                            start = range.first,
                            end = range.last + 1, // make end exclusive in model
                            text = text,
                            embedding = embeddings[idx]
                        )
                    }

                    Document(
                        id = entry.relativePath,
                        path = entry.relativePath,
                        title = deriveTitle(content),
                        sha256 = hasher.sha256(content),
                        chunks = chunks
                    )
                }

                docs.add(doc)

                // Hint for GC - free memory after processing large files
                if ((fileIndex + 1) % 10 == 0) {
                    println("   🧹 Memory cleanup...")
                }
            }
        }

        val avgLen = if (totalChunks > 0) totalChunkLen.toDouble() / totalChunks else 0.0

        val elapsedMs = (Clock.System.now().nanosecondsOfSecond - startNs) / 1_000_000

        println("\n" + "=".repeat(50))
        println("✨ Indexing completed!")
        println("📊 Statistics:")
        println("   • Documents: ${docs.size}")
        println("   • Chunks: $totalChunks")
        println("   • Avg chunk length: ${avgLen.toInt()} chars")
        println("   • Time: ${elapsedMs / 1000.0}s")
        println("=".repeat(50))

        return DocumentIndex(
            version = "1",
            builtAt = DocumentIndex.nowIso(),
            source = SourceConfig(inputDir = inputDir),
            model = model,
            params = params,
            documents = docs,
            stats = IndexStats(
                docs = docs.size,
                chunks = totalChunks,
                avgChunkLen = avgLen,
                elapsedMs = elapsedMs.toLong()
            )
        )
    }

    private fun normalize(text: String): String {
        // Нормализуем переводы строк и схлопываем избыточные пустые строки до максимум 2 подряд
        val unix = text.replace("\r\n", "\n").replace("\r", "\n")
        val lines = unix.split('\n')
        val out = StringBuilder(unix.length)
        var emptyInRow = 0
        for (line in lines) {
            if (line.isBlank()) {
                emptyInRow++
            } else {
                emptyInRow = 0
            }
            if (emptyInRow <= 2) {
                out.append(line).append('\n')
            }
        }
        return out.toString().trim()
    }

    private fun safeSubstring(text: String, range: IntRange): String {
        val start = range.first.coerceIn(0, text.length)
        val endExclusive = (range.last + 1).coerceIn(0, text.length)
        return if (endExclusive > start) text.substring(start, endExclusive) else ""
    }

    private fun deriveTitle(text: String): String? {
        // Простая эвристика: первая строка, начинающаяся с '#'
        val firstHeader = text.lineSequence()
            .firstOrNull { it.trimStart().startsWith("#") }
            ?.trim()
        return firstHeader
    }
}

/**
 * Утилита для вычисления char-ориентированных параметров из токенных.
 */
fun computeCharWindow(targetTokens: Int, overlapTokens: Int, charsPerToken: Double): Pair<Int, Int> {
    val maxChars = (targetTokens * charsPerToken).roundToInt().coerceAtLeast(1)
    val overlapChars = (overlapTokens * charsPerToken).roundToInt().coerceAtLeast(0).coerceAtMost(maxChars - 1)
    return maxChars to overlapChars
}
