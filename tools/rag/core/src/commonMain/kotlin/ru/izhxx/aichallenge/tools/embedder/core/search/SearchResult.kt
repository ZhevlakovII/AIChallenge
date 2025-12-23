package ru.izhxx.aichallenge.tools.embedder.core.search

import ru.izhxx.aichallenge.tools.embedder.core.chunks.Chunk

class SearchResult(
    val chunk: Chunk,
    val score: Float,
    val scoreBreakdown: ScoreBreakdown? = null
)
