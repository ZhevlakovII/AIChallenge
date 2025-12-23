package ru.izhxx.aichallenge.tools.embedder.core.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object IdGenerator {

    fun documentId(): String = "doc_${uuid()}"
    fun chunkId(documentId: String, index: Int): String = "${documentId}_chunk_$index"
    fun collectionId(): String = "col_${uuid()}"

    private fun uuid(): String = Uuid.random().toString().replace("-", "").take(12)
}