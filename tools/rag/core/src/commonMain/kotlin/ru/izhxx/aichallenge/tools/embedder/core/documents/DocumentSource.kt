package ru.izhxx.aichallenge.tools.embedder.core.documents

sealed interface DocumentSource {
    class File(val path: String, val hash: String) : DocumentSource
    class Url(val url: String) : DocumentSource
    class Raw(val identifier: String) : DocumentSource
}
