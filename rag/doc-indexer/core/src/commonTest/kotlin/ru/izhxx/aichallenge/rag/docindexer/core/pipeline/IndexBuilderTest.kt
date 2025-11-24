package ru.izhxx.aichallenge.rag.docindexer.core.pipeline

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ru.izhxx.aichallenge.rag.docindexer.core.api.ContentReader
import ru.izhxx.aichallenge.rag.docindexer.core.api.Embedder
import ru.izhxx.aichallenge.rag.docindexer.core.api.Hasher
import ru.izhxx.aichallenge.rag.docindexer.core.api.TextChunker
import ru.izhxx.aichallenge.rag.docindexer.core.impl.CharOverlapChunker
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildParams
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildRequest
import ru.izhxx.aichallenge.rag.docindexer.core.model.FileEntry
import ru.izhxx.aichallenge.rag.docindexer.core.model.ModelConfig

class IndexBuilderTest {

    @Test
    fun build_normalizes_and_chunks_and_embeds() = runTest {
        // Готовим вход с CRLF и избытком пустых строк
        val raw = buildString {
            appendLine("# Doc\r")
            appendLine("\r")
            appendLine("\r")
            appendLine("Line1\r")
            appendLine("\r")
            append("Line2")
        }
        val files = mapOf("doc.md" to raw)

        val reader = FakeReader(files)
        val chunker: TextChunker = CharOverlapChunker()
        val embedder = FakeEmbedder(listOf(0.1, 0.2, 0.3))
        val hasher = FakeHasher()

        val (maxChars, overlapChars) = computeCharWindow(
            targetTokens = 50,
            overlapTokens = 10,
            charsPerToken = 3.0
        )

        val params = BuildParams(
            targetTokens = 50,
            overlapTokens = 10,
            charsPerToken = 3.0,
            maxChars = maxChars,
            overlapChars = overlapChars,
            concurrency = 2
        )
        val req = BuildRequest(
            inputDir = "ignored",
            params = params,
            model = ModelConfig(
                name = "test-embed-model",
                endpoint = "http://localhost:11434"
            )
        )

        val index = IndexBuilder(
            reader = reader,
            chunker = chunker,
            embedder = embedder,
            hasher = hasher
        ).build(req)

        // Проверки индекса
        assertTrue(index.stats.docs == 1, "Должен быть 1 документ")
        assertTrue(index.stats.chunks >= 1, "Должен быть >= 1 чанка")
        assertEquals("ignored", index.source.inputDir)
        assertEquals("test-embed-model", index.model.name)

        val doc = index.documents.first()
        assertEquals("doc.md", doc.id)
        assertEquals("doc.md", doc.path)
        assertEquals("# Doc", doc.title, "Заголовок должен быть считан из первой строки, начинающейся с '#'")
        assertTrue(doc.sha256.isNotEmpty(), "sha256 должен быть заполнен")

        val firstChunk = doc.chunks.first()
        val t = firstChunk.text
        // 1) Нормализация переводов строк: без '\r'
        assertTrue(!t.contains('\r'), "Текст чанка не должен содержать CR ('\\r')")
        // 2) Схлопывание пустых строк: максимум 2 подряд — то есть не должно быть трёх и более
        assertTrue(!t.contains("\n\n\n"), "В тексте чанка не должно быть трёх и более подряд идущих переводов строк")
        // 3) Эмбеддинг получен
        assertNotNull(firstChunk.embedding)
        assertTrue(firstChunk.embedding.isNotEmpty(), "Эмбеддинг должен быть непустым")
        // 4) Диапазоны валидны
        val endExclusive = firstChunk.end
        val length = endExclusive - firstChunk.start
        assertTrue(length > 0, "Длина первого чанка должна быть > 0")
        assertTrue(length <= maxChars, "Длина первого чанка должна быть <= maxChars")
    }

    // ---- Тестовые реализации портов ----

    private class FakeReader(
        private val files: Map<String, String>
    ) : ContentReader {
        override fun scanMarkdownFiles(rootDir: String): List<FileEntry> {
            return files.keys.map { name ->
                FileEntry(
                    absolutePath = "/abs/$name",
                    relativePath = name
                )
            }
        }

        override fun read(entry: FileEntry): String {
            return files[entry.relativePath] ?: error("No content for ${entry.relativePath}")
        }
    }

    private class FakeEmbedder(
        private val vector: List<Double>
    ) : Embedder {
        override suspend fun embed(text: String): List<Double> = vector
    }

    private class FakeHasher : Hasher {
        override fun sha256(text: String): String = "hash-${
            text.length
        }"
    }
}
