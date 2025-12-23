package ru.izhxx.aichallenge.tools.embedder.core.documents

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DocumentMetadata(
    val title: String,
    val author: String?,
    val createdAt: Instant,
    val modifiedAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val type: DocumentType = DocumentType.UNKNOWN,
    val language: String? = null,
    val custom: Map<String, String> = emptyMap()
)
