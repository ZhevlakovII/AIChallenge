package ru.izhxx.aichallenge.tools.embedder.core.chunks

class ChunkMetadata(
    val level: ChunkLevel,
    val parentId: String? = null,
    val section: String?,       // Заголовок секции для MD/HTML
    val pageNumber: Int?,       // Для PDF
    val tokenCount: Int?
)
