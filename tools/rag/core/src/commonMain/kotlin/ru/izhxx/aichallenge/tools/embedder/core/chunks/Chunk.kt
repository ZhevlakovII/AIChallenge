package ru.izhxx.aichallenge.tools.embedder.core.chunks

class Chunk(
    val id: String,
    val documentId: String,
    val content: String,
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: ChunkMetadata
)
