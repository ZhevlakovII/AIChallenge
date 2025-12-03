package ru.izhxx.aichallenge.data.rag

import ru.izhxx.aichallenge.domain.rag.CutoffMode
import ru.izhxx.aichallenge.domain.rag.RagEmbedder
import ru.izhxx.aichallenge.domain.rag.RagReranker
import ru.izhxx.aichallenge.domain.rag.RagRetriever
import ru.izhxx.aichallenge.domain.rag.RagSettings
import ru.izhxx.aichallenge.domain.rag.RerankMode
import ru.izhxx.aichallenge.domain.rag.RerankSettings
import ru.izhxx.aichallenge.domain.rag.RetrievedChunk
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Оркестрация 2-ступенчатого поиска:
 *  1) kNN кандидаты от retriever (candidateK, minScore)
 *  2) опциональный rerank (MMR/LLM)
 *  3) cutoff нерелевантных
 *  4) усечение до topK
 */
class RagSearchPipeline(
    private val embedder: RagEmbedder,
    private val retriever: RagRetriever,
    /**
     * Фабрика реранкера (позволяет подставлять разные реализации).
     * Для MMR можно вернуть MmrReranker(), для LLM — свою реализацию, или null если не настроено.
     */
    private val rerankerFactory: (RerankMode) -> RagReranker? = { mode ->
        when (mode) {
            RerankMode.MMR -> MmrReranker()
            RerankMode.LLM -> null // локальный LLM-reranker не настроен по умолчанию
            RerankMode.None -> null
        }
    }
) {

    suspend fun retrieveChunks(
        questionText: String,
        index: DocumentIndex,
        settings: RagSettings
    ): List<RetrievedChunk> {
        // 1) question embedding
        val qEmb = embedder.embed(questionText)
        if (qEmb.isEmpty()) return emptyList()

        // 2) кандидаты (быстрый поиск)
        val candidateK = max(settings.topK, settings.rerank.candidateK).coerceAtLeast(1)
        val candidates = retriever.retrieve(
            questionEmbedding = qEmb,
            index = index,
            topK = candidateK,
            minScore = settings.minScore
        )

        // 3) rerank (опционально)
        val reranked = when (settings.rerank.mode) {
            RerankMode.None -> candidates
            RerankMode.MMR, RerankMode.LLM -> {
                val rr = rerankerFactory(settings.rerank.mode)
                rr?.rerank(
                    questionEmbedding = qEmb,
                    candidates = candidates,
                    index = index,
                    settings = settings.rerank
                )
                    ?: candidates // фоллбэк, если реранкер не предоставлен
            }
        }

        // 4) cutoff
        val filtered = applyCutoff(reranked, settings.rerank)

        // 5) финальные topK
        return filtered.take(settings.topK.coerceAtLeast(1))
    }

    fun buildContext(
        chunks: List<RetrievedChunk>,
        index: DocumentIndex,
        settings: RagSettings
    ): String = retriever.buildContext(
        chunks = chunks,
        index = index,
        maxTokens = settings.maxContextTokens
    )

    suspend fun retrieveAndBuildContext(
        questionText: String,
        index: DocumentIndex,
        settings: RagSettings
    ): String {
        val chunks = retrieveChunks(questionText, index, settings)
        return buildContext(chunks, index, settings)
    }

    private fun applyCutoff(
        items: List<RetrievedChunk>,
        rs: RerankSettings
    ): List<RetrievedChunk> {
        if (items.isEmpty()) return items

        return when (rs.cutoffMode) {
            CutoffMode.Static -> {
                val thr = rs.minRerankScore
                if (thr == null) items else items.filter { it.score >= thr }
            }
            CutoffMode.Quantile -> {
                val q = rs.quantileQ.coerceIn(0.0, 1.0)
                if (q <= 0.0) return items
                if (items.size < 2) return items
                val sortedScores = items.map { it.score }.sorted()
                val idx = (sortedScores.size * q).toInt().coerceIn(0, sortedScores.lastIndex)
                val thr = sortedScores[idx]
                items.filter { it.score >= thr }
            }
            CutoffMode.ZScore -> {
                if (items.size < 2) return items
                val mean = items.sumOf { it.score } / items.size
                var s2 = 0.0
                for (c in items) {
                    val d = c.score - mean
                    s2 += d * d
                }
                val std = sqrt(s2 / (items.size - 1).coerceAtLeast(1))
                if (std == 0.0) return items
                val thrZ = rs.zScore
                items.filter { (it.score - mean) / std >= thrZ }
            }
        }
    }
}
