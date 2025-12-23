package ru.izhxx.aichallenge.tools.embedder.core.collections

class Collection(
    val id: String,
    val name: String,
    val description: String? = null,
    val config: CollectionConfig,
    val stats: CollectionStats
)
