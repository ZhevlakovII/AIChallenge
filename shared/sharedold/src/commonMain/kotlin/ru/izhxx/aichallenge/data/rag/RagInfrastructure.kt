package ru.izhxx.aichallenge.data.rag

import io.ktor.client.HttpClient
import kotlin.math.sqrt
import ru.izhxx.aichallenge.domain.rag.RagEmbedder
import ru.izhxx.aichallenge.domain.rag.RagRetriever
import ru.izhxx.aichallenge.domain.rag.RetrievedChunk
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import ru.izhxx.aichallenge.rag.docindexer.ollama.OllamaEmbedder

/**
 * Обертка над OllamaEmbedder для RAG рантайма.
 */
class RagOllamaEmbedder(
    httpClient: HttpClient,
    baseUrl: String = "http://localhost:11434",
    model: String = "mxbai-embed-large",
    retries: Int = 3,
    initialBackoffMs: Long = 250
) : RagEmbedder {
    private val delegate = OllamaEmbedder(
        http = httpClient,
        baseUrl = baseUrl,
        model = model,
        retries = retries,
        initialBackoffMs = initialBackoffMs
    )

    override suspend fun embed(text: String): List<Double> = delegate.embed(text)
}

/**
 * Простой kNN-ретривер по косинусной близости.
 */
class DefaultRagRetriever : RagRetriever {

    override suspend fun retrieve(
        questionEmbedding: List<Double>,
        index: DocumentIndex,
        topK: Int,
        minScore: Double
    ): List<RetrievedChunk> {
        if (questionEmbedding.isEmpty()) return emptyList()

        val qNorm = l2(questionEmbedding)
        if (qNorm == 0.0) return emptyList()

        val results = mutableListOf<RetrievedChunk>()
        index.documents.forEach { doc ->
            doc.chunks.forEach { ch ->
                // Защита от рассогласования размерности
                if (ch.embedding.size != questionEmbedding.size) return@forEach
                val score = cosine(questionEmbedding, qNorm, ch.embedding)
                if (score >= minScore) {
                    results += RetrievedChunk(
                        docId = doc.id,
                        path = doc.path,
                        chunkIndex = ch.index,
                        score = score,
                        text = ch.text
                    )
                }
            }
        }
        return results
            .sortedByDescending { it.score }
            .take(topK.coerceAtLeast(1))
    }

    override fun buildContext(
        chunks: List<RetrievedChunk>,
        index: DocumentIndex,
        maxTokens: Int
    ): String {
        if (chunks.isEmpty() || maxTokens <= 0) return ""

        val charsPerToken = index.params.charsPerToken
        val maxCharsBudget = if (charsPerToken > 0.0) {
            (maxTokens * charsPerToken).toInt().coerceAtLeast(1)
        } else {
            // fallback эвристика
            (maxTokens * 3).coerceAtLeast(1)
        }

        val sb = StringBuilder()
        sb.appendLine("[CONTEXT]")
        var usedChars = 0
        for (c in chunks) {
            val header = "Source: ${c.path}#${c.chunkIndex} (score=${"%.3f".format(c.score)})"
            val block = buildString {
                appendLine(header)
                appendLine(c.text.trim())
                appendLine()
            }
            if (usedChars + block.length > maxCharsBudget) break
            sb.append(block)
            usedChars += block.length
        }
        sb.appendLine("[/CONTEXT]")
        sb.appendLine("Инструкция: Отвечай, опираясь только на CONTEXT выше. Если в CONTEXT нет ответа — явно сообщи об этом.")
        return sb.toString()
    }

    private fun l2(v: List<Double>): Double {
        var s = 0.0
        for (x in v) s += x * x
        return sqrt(s)
    }

    private fun cosine(a: List<Double>, aNorm: Double, b: List<Double>): Double {
        var dot = 0.0
        var i = 0
        while (i < a.size) {
            dot += a[i] * b[i]
            i++
        }
        val bNorm = l2(b)
        if (aNorm == 0.0 || bNorm == 0.0) return 0.0
        return dot / (aNorm * bNorm)
    }
}
