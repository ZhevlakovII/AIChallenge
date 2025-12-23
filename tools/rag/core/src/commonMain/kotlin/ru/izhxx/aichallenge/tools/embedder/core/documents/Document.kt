package ru.izhxx.aichallenge.tools.embedder.core.documents

class Document(
    val id: String,
    val content: String,
    val metadata: DocumentMetadata,
    val source: DocumentSource? = null
)
