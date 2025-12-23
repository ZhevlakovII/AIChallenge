package ru.izhxx.aichallenge.tools.embedder.core.embeddings

import ru.izhxx.aichallenge.tools.embedder.core.chunks.Chunk

class EmbeddedChunk(
    val chunk: Chunk,
    val embedding: Embedding
)
