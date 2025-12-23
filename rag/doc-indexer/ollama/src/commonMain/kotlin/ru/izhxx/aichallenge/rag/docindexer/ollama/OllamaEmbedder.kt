package ru.izhxx.aichallenge.rag.docindexer.ollama

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.izhxx.aichallenge.rag.docindexer.core.api.Embedder

/**
 * Общий (common) Embedder для Ollama. Требует, чтобы в переданном HttpClient был установлен
 * ContentNegotiation с kotlinx.serialization(JSON) и тайм-ауты на уровне клиента.
 *
 * Поддерживает два формата ответа:
 * 1) {"embedding": [Double]}
 * 2) {"embeddings": [[Double], ...]}  // батч; берём первый элемент
 */
class OllamaEmbedder(
    private val http: HttpClient,
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "mxbai-embed-large",
    private val retries: Int = 3,
    private val initialBackoffMs: Long = 250
) : Embedder {

    @Serializable
    private data class EmbeddingRequest(
        val model: String,
        val prompt: String? = null,
        val input: String? = null,
        val inputs: List<String>? = null
    )

    @Serializable
    private data class EmbeddingResponse(
        val model: String? = null,
        val embedding: List<Double>? = null,
        // некоторые реализации используют поле "embeddings" для батча
        val embeddings: List<List<Double>>? = null,
        // на случай других расширений API
        val data: List<EmbeddingData>? = null,
        val error: String? = null,
        val message: String? = null
    )

    @Serializable
    private data class EmbeddingData(
        @SerialName("embedding")
        val embedding: List<Double>
    )

    override suspend fun embed(text: String): List<Double> {
        var attempt = 0
        var backoff = initialBackoffMs
        var lastError: Throwable? = null

        suspend fun request(body: EmbeddingRequest): EmbeddingResponse {
            println("         [Ollama] POST $baseUrl/api/embeddings (text=${text.take(30).replace("\n", " ")}...)")
            return http.post("$baseUrl/api/embeddings") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }

        fun extract(resp: EmbeddingResponse): List<Double>? {
            if (resp.error != null || resp.message != null) {
                throw IllegalStateException("Ollama error: ${resp.error ?: resp.message}")
            }
            resp.embedding?.let { if (it.isNotEmpty()) return it }
            resp.embeddings?.firstOrNull()?.let { if (it.isNotEmpty()) return it }
            resp.data?.firstOrNull()?.embedding?.let { if (it.isNotEmpty()) return it }
            return null
        }

        while (attempt <= retries) {
            try {
                // 1) prompt
                extract(request(EmbeddingRequest(model = model, prompt = text)))?.let {
                    println("         [Ollama] ✓ Received embedding")
                    return it
                }
                // 2) input (string)
                extract(request(EmbeddingRequest(model = model, input = text)))?.let {
                    println("         [Ollama] ✓ Received embedding")
                    return it
                }
                // 3) inputs (array)
                extract(request(EmbeddingRequest(model = model, inputs = listOf(text))))?.let {
                    println("         [Ollama] ✓ Received embedding")
                    return it
                }

                error("Ollama embeddings response doesn't contain embedding(s) or returned empty vector")
            } catch (t: Throwable) {
                lastError = t
                println("         [Ollama] ⚠️  Attempt ${attempt + 1}/${retries + 1} failed: ${t.message}")
                if (attempt == retries) break
                println("         [Ollama] 🔄 Retrying in ${backoff}ms...")
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(4000)
                attempt++
            }
        }
        println("         [Ollama] ❌ All attempts failed")
        throw lastError ?: IllegalStateException("Unknown error in OllamaEmbedder")
    }
}
