package ru.izhxx.aichallenge.tools.embedder.api

import ru.izhxx.aichallenge.tools.embedder.core.embeddings.Embedding

interface EmbeddingsProvider {

    suspend fun embed(text: String): Embedding
    suspend fun embedBatch(texts: List<String>): List<Embedding>
}
