package ru.izhxx.aichallenge.rag.docindexer.core.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Chunk(
    val id: String,
    val index: Int,
    val start: Int,
    val end: Int,
    val text: String,
    val embedding: List<Double>
)

@Serializable
data class Document(
    val id: String,           // будем использовать относительный путь как идентификатор
    val path: String,         // относительный путь от корня сканирования
    val title: String? = null,
    val sha256: String,
    val chunks: List<Chunk>
)

@Serializable
data class SourceConfig(
    val inputDir: String
)

@Serializable
data class ModelConfig(
    val provider: String = "ollama",
    val name: String,
    val endpoint: String,
    val dim: Int? = null
)

@Serializable
data class BuildParams(
    val targetTokens: Int,
    val overlapTokens: Int,
    val charsPerToken: Double,
    val maxChars: Int,
    val overlapChars: Int,
    val concurrency: Int
)

@Serializable
data class IndexStats(
    val docs: Int,
    val chunks: Int,
    val avgChunkLen: Double,
    val elapsedMs: Long
)

@Serializable
data class DocumentIndex(
    val version: String = "1",
    val builtAt: String,
    val source: SourceConfig,
    val model: ModelConfig,
    val params: BuildParams,
    val documents: List<Document>,
    val stats: IndexStats
) {
    companion object {
        fun nowIso(): String = Clock.System.now().toString() // ISO-8601
    }
}

// ---- Build request DTOs (используются пайплайном) ----

data class BuildRequest(
    val inputDir: String,
    val params: BuildParams,
    val model: ModelConfig
)

data class FileEntry(
    val absolutePath: String,
    val relativePath: String
)
