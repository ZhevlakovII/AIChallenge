# :tool:rag:core

Базовые модели и абстракции, используемые всеми RAG-компонентами.

**Зависимости:** `:core:common`, `:core:model`

---

## Документы

```kotlin
/**
 * Исходный документ для индексации
 */
data class Document(
    val id: String,
    val content: String,
    val metadata: DocumentMetadata = DocumentMetadata(),
    val source: DocumentSource? = null
)

data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val createdAt: Instant? = null,
    val modifiedAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val type: DocumentType = DocumentType.UNKNOWN,
    val language: String? = null,
    val custom: Map<String, String> = emptyMap()
)

enum class DocumentType {
    MARKDOWN,
    HTML,
    PDF,
    DOCX,
    PLAIN_TEXT,
    UNKNOWN
}

sealed class DocumentSource {
    data class File(val path: String, val hash: String) : DocumentSource()
    data class Url(val url: String) : DocumentSource()
    data class Raw(val identifier: String) : DocumentSource()
}
```

---

## Чанки

```kotlin
/**
 * Результат чанкования документа
 */
data class Chunk(
    val id: String,
    val documentId: String,
    val content: String,
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: ChunkMetadata = ChunkMetadata()
)

data class ChunkMetadata(
    val level: ChunkLevel = ChunkLevel.CHILD,
    val parentId: String? = null,
    val section: String? = null,       // Заголовок секции для MD/HTML
    val pageNumber: Int? = null,       // Для PDF
    val tokenCount: Int? = null
)

enum class ChunkLevel {
    PARENT,     // Большой чанк для контекста
    CHILD       // Маленький чанк для точного поиска
}
```

---

## Эмбеддинги

```kotlin
/**
 * Чанк с вычисленным эмбеддингом
 */
data class EmbeddedChunk(
    val chunk: Chunk,
    val embedding: Embedding
)

// Embedding из :core:model
// @JvmInline value class Embedding(val vector: FloatArray)
```

---

## Результаты поиска

```kotlin
data class SearchResult(
    val chunk: Chunk,
    val score: Float,
    val scoreBreakdown: ScoreBreakdown? = null
)

data class ScoreBreakdown(
    val semanticScore: Float? = null,
    val keywordScore: Float? = null,
    val rerankScore: Float? = null
)
```

---

## Коллекции

```kotlin
/**
 * Независимая коллекция документов
 */
data class Collection(
    val id: String,
    val name: String,
    val description: String? = null,
    val config: CollectionConfig = CollectionConfig(),
    val stats: CollectionStats = CollectionStats()
)

data class CollectionConfig(
    val embeddingDimensions: Int = 384,
    val distanceMetric: DistanceMetric = DistanceMetric.COSINE
)

data class CollectionStats(
    val documentCount: Int = 0,
    val chunkCount: Int = 0,
    val lastUpdated: Instant? = null
)

enum class DistanceMetric {
    COSINE,
    EUCLIDEAN,
    DOT_PRODUCT
}
```

---

## Утилиты

```kotlin
/**
 * Хэширование для кэша и определения изменений
 */
object ContentHash {
    fun compute(content: String): String {
        return content.encodeToByteArray()
            .let { MessageDigest.getInstance("SHA-256").digest(it) }
            .joinToString("") { "%02x".format(it) }
    }
    
    fun compute(content: String, modelId: String): String {
        return compute("$modelId:$content")
    }
}

/**
 * ID генерация
 */
object IdGenerator {
    fun documentId(): String = "doc_${uuid()}"
    fun chunkId(documentId: String, index: Int): String = "${documentId}_chunk_$index"
    fun collectionId(): String = "col_${uuid()}"
    
    private fun uuid(): String = UUID.randomUUID().toString().replace("-", "").take(12)
}
```
