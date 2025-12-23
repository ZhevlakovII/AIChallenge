package ru.izhxx.aichallenge.tools.embedder.core.utils

object ContentHasher {
    fun compute(content: String): String {
        return content.encodeToByteArray().sha256()
    }

    fun compute(content: String, modelId: String): String {
        return compute("$modelId:$content")
    }
}
