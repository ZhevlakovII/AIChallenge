package ru.izhxx.aichallenge.tools.embedder.core.collections

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class CollectionStats(
    val documentCount: Int,
    val chunkCount: Int,
    val lastUpdated: Instant?
)