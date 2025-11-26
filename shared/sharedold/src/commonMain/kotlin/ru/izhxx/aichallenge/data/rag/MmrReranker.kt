package ru.izhxx.aichallenge.data.rag

import ru.izhxx.aichallenge.domain.rag.RagReranker
import ru.izhxx.aichallenge.domain.rag.RerankSettings
import ru.izhxx.aichallenge.domain.rag.RetrievedChunk
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import kotlin.math.sqrt

/**
 * Локальный MMR-реранкер.
 *
 * Формула (жадная):
 *   score(d) = λ * sim(q, d) - (1 - λ) * max_{s ∈ S} sim(d, s)
 * где:
 *   - sim(q, d) — косинусная близость вопроса и кандидата (берём из candidates[i].score)
 *   - sim(d, s) — косинусная близость между эмбеддингами чанков d и s
 *   - S — уже выбранные документы
 *
 * Возвращает кандидатов в переупорядоченном виде. Сами значения score у RetrievedChunk не меняются.
 */
class MmrReranker : RagReranker {

    override suspend fun rerank(
        questionEmbedding: List<Double>,
        candidates: List<RetrievedChunk>,
        index: DocumentIndex,
        settings: RerankSettings
    ): List<RetrievedChunk> {
        if (candidates.size <= 1) return candidates
        val lambda = settings.mmrLambda.coerceIn(0.0, 1.0)

        // Кэш нормированных эмбеддингов для кандидатов
        val embCache = HashMap<Int, DoubleArray>()

        fun fetchEmbedding(i: Int): DoubleArray? {
            return embCache.getOrPut(i) {
                val c = candidates[i]
                val chunkEmbedding: List<Double>? = findChunkEmbedding(index, c)
                if (chunkEmbedding == null) {
                    DoubleArray(0)
                } else {
                    val arr = DoubleArray(chunkEmbedding.size) { idx -> chunkEmbedding[idx] }
                    val norm = l2(arr)
                    if (norm == 0.0) {
                        DoubleArray(0)
                    } else {
                        for (k in arr.indices) arr[k] /= norm
                        arr
                    }
                }
            }.let { if (it.isEmpty()) null else it }
        }

        // sim(q, d) — используем уже рассчитанный retriever'ом score
        val qsim = DoubleArray(candidates.size) { i -> candidates[i].score }

        val selected = ArrayList<Int>(candidates.size)
        val remaining = java.util.LinkedHashSet<Int>((0 until candidates.size).toList())

        while (remaining.isNotEmpty()) {
            var bestIdx = -1
            var bestScore = Double.NEGATIVE_INFINITY

            for (i in remaining) {
                val rel = qsim[i]
                var div = 0.0
                if (selected.isNotEmpty()) {
                    val ei = fetchEmbedding(i)
                    if (ei != null) {
                        var maxSim = 0.0
                        for (j in selected) {
                            val ej = fetchEmbedding(j)
                            if (ej != null) {
                                val sim = dot(ei, ej) // т.к. эмбеддинги нормированы, dot = cosine
                                if (sim > maxSim) maxSim = sim
                            }
                        }
                        div = maxSim
                    }
                }
                val mmr = lambda * rel - (1.0 - lambda) * div
                if (mmr > bestScore) {
                    bestScore = mmr
                    bestIdx = i
                }
            }

            if (bestIdx == -1) break
            selected.add(bestIdx)
            remaining.remove(bestIdx)
        }

        // Переупорядоченный список
        return selected.map { candidates[it] }
    }

    private fun findChunkEmbedding(index: DocumentIndex, c: RetrievedChunk): List<Double>? {
        // Сначала пробуем путь, затем docId
        val doc = index.documents.firstOrNull { it.path == c.path }
            ?: index.documents.firstOrNull { it.id == c.docId }
            ?: return null

        if (c.chunkIndex !in doc.chunks.indices) return null
        return doc.chunks[c.chunkIndex].embedding
    }

    private fun l2(v: DoubleArray): Double {
        var s = 0.0
        for (x in v) s += x * x
        return sqrt(s)
    }

    private fun dot(a: DoubleArray, b: DoubleArray): Double {
        val n = minOf(a.size, b.size)
        var s = 0.0
        var i = 0
        while (i < n) {
            s += a[i] * b[i]
            i++
        }
        return s
    }
}
