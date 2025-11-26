package ru.izhxx.aichallenge.data.rag

import ru.izhxx.aichallenge.domain.rag.RagEmbedder
import ru.izhxx.aichallenge.domain.rag.RagRetriever
import ru.izhxx.aichallenge.domain.rag.RagSettings
import ru.izhxx.aichallenge.domain.rag.RerankMode
import ru.izhxx.aichallenge.domain.rag.RetrievedChunk
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import kotlin.math.log2
import kotlin.system.getTimeMillis

/**
 * Мини-харнесс оценки качества RAG.
 *
 * Формат ожидаемых релевантов:
 * - Рекомендуется: "path#chunkIndex", напр. "memory-bank/sessions/2025-11-21.md#0"
 * - Поддерживаются также: "docId#chunkIndex", просто "path" или "docId" (совпадение по документу без учета чанка).
 */
data class EvalCase(
    val q: String,
    val relevant: List<String>
)

data class EvalSummary(
    val size: Int,
    val hitAtK: Double,
    val mrr: Double,
    val ndcg: Double,
    val avgLatencyMs: Double
)

object RagEval {

    /**
     * Сравнение baseline (RerankMode.None) и MMR (RerankMode.MMR) на одном и том же наборе кейсов.
     * Возвращает пары (mode -> summary).
     *
     * Замечание: embedder/retriever/pipeline передаются снаружи, чтобы не привязываться к конкретной имплементации.
     */
    suspend fun evaluateBaselineVsMmr(
        cases: List<EvalCase>,
        index: DocumentIndex,
        baseSettings: RagSettings,
        embedder: RagEmbedder,
        retriever: RagRetriever
    ): Map<RerankMode, EvalSummary> {
        val pipeline = RagSearchPipeline(
            embedder = embedder,
            retriever = retriever
        )

        val baseline = baseSettings.copy(rerank = baseSettings.rerank.copy(mode = RerankMode.None))
        val mmr = baseSettings.copy(rerank = baseSettings.rerank.copy(mode = RerankMode.MMR))

        val baseRes = evaluateMode(cases, index, baseline, pipeline)
        val mmrRes = evaluateMode(cases, index, mmr, pipeline)

        return mapOf(
            RerankMode.None to baseRes,
            RerankMode.MMR to mmrRes
        )
    }

    private suspend fun evaluateMode(
        cases: List<EvalCase>,
        index: DocumentIndex,
        settings: RagSettings,
        pipeline: RagSearchPipeline
    ): EvalSummary {
        if (cases.isEmpty()) return EvalSummary(0, 0.0, 0.0, 0.0, 0.0)

        var hits = 0
        var sumRR = 0.0
        var sumDCG = 0.0
        var sumLatency = 0L
        val k = settings.topK.coerceAtLeast(1)
        val idcg = idealDcg(k = k, relCount = averageRelevantCount(cases))

        for (c in cases) {
            val t0 = getTimeMillis()
            val chunks = pipeline.retrieveChunks(c.q, index, settings)
            sumLatency += (getTimeMillis() - t0)
            val eval = evaluateOne(c, chunks, k)
            if (eval.hit) hits++
            sumRR += eval.rr
            sumDCG += eval.dcg
        }

        val n = cases.size
        val hitAtK = hits.toDouble() / n
        val mrr = sumRR / n
        val ndcg = if (idcg > 0.0) (sumDCG / n) / idcg else 0.0
        val avgLatency = sumLatency.toDouble() / n
        return EvalSummary(
            size = n,
            hitAtK = hitAtK,
            mrr = mrr,
            ndcg = ndcg,
            avgLatencyMs = avgLatency
        )
    }

    private data class OneEval(val hit: Boolean, val rr: Double, val dcg: Double)

    private fun evaluateOne(
        c: EvalCase,
        retrieved: List<RetrievedChunk>,
        k: Int
    ): OneEval {
        val relSet = c.relevant.toSet()
        var hit = false
        var rr = 0.0
        var dcg = 0.0

        val limit = minOf(k, retrieved.size)
        for (i in 0 until limit) {
            val r = retrieved[i]
            val atLeastDocMatch = isRelevant(relSet, r)
            val gain = if (atLeastDocMatch) 1.0 else 0.0
            if (gain > 0 && !hit) {
                hit = true
                rr = 1.0 / (i + 1)
            }
            if (gain > 0) {
                dcg += gain / log2((i + 1) + 1.0) // log2(i+2)
            }
        }
        return OneEval(hit, rr, dcg)
    }

    private fun isRelevant(relSet: Set<String>, c: RetrievedChunk): Boolean {
        val k1 = "${c.path}#${c.chunkIndex}"
        val k2 = "${c.docId}#${c.chunkIndex}"
        // также разрешаем совпадение по документу без учета индекса
        val k3 = c.path
        val k4 = c.docId
        return k1 in relSet || k2 in relSet || k3 in relSet || k4 in relSet
    }

    /**
     * Простейшая оценка "ожидаемого" IDCG: берём среднее число релевантов среди кейсов и считаем,
     * как будто идеальная выдача положила их в топ. Это грубая аппроксимация для нормировки.
     */
    private fun idealDcg(k: Int, relCount: Double): Double {
        val m = minOf(k.toDouble(), relCount)
        var s = 0.0
        var i = 1
        while (i <= m.toInt()) {
            s += 1.0 / log2(i + 1.0)
            i++
        }
        return s
    }

    private fun averageRelevantCount(cases: List<EvalCase>): Double {
        if (cases.isEmpty()) return 0.0
        var sum = 0.0
        for (c in cases) sum += c.relevant.size
        return sum / cases.size
    }
}

/**
 * Пример использования (псевдокод):
 *
 * val http = HttpClient()
 * val embedder = RagOllamaEmbedder(httpClient = http, baseUrl = "...", model = "mxbai-embed-large")
 * val retriever = DefaultRagRetriever()
 * val settings = RagSettings(
 *   enabled = true,
 *   indexPath = "...",
 *   topK = 4,
 *   minScore = 0.3,
 *   maxContextTokens = 1024,
 *   rerank = RerankSettings(mode = RerankMode.MMR, candidateK = 16, mmrLambda = 0.5)
 * )
 * val cases = listOf(
 *   EvalCase(q = "Как запустить индексатор?", relevant = listOf("rag/README.md#0", "rag/doc-indexer/app/README.md")),
 *   ...
 * )
 * val index: DocumentIndex = ... // загрузите через RagIndexRepository
 * val res = RagEval.evaluateBaselineVsMmr(cases, index, settings, embedder, retriever)
 * println(res)
 */
