# :tool:rag:storage

Векторное хранилище с поддержкой FTS для hybrid search.

**Зависимости:** `:tool:rag:core`, `:core:common`, `:core:database`

---

## Абстракции

### Vector Store

```kotlin
/**
 * Векторное хранилище
 */
interface VectorStore {
    // Коллекции
    suspend fun createCollection(collection: Collection)
    suspend fun deleteCollection(collectionId: String)
    suspend fun getCollection(collectionId: String): Collection?
    suspend fun listCollections(): List<Collection>
    
    // Чанки
    suspend fun add(collectionId: String, chunks: List<EmbeddedChunk>)
    suspend fun update(collectionId: String, chunks: List<EmbeddedChunk>)
    suspend fun delete(collectionId: String, chunkIds: List<String>)
    suspend fun deleteByDocument(collectionId: String, documentId: String)
    
    suspend fun get(collectionId: String, chunkId: String): EmbeddedChunk?
    suspend fun getByDocument(collectionId: String, documentId: String): List<EmbeddedChunk>
    
    // Поиск
    suspend fun searchSemantic(
        collectionId: String,
        query: Embedding,
        topK: Int,
        filter: MetadataFilter? = null
    ): List<SearchResult>
}
```

### FTS Store

```kotlin
/**
 * Full-Text Search хранилище
 */
interface FtsStore {
    suspend fun index(collectionId: String, chunks: List<Chunk>)
    suspend fun delete(collectionId: String, chunkIds: List<String>)
    suspend fun deleteByDocument(collectionId: String, documentId: String)
    
    suspend fun search(
        collectionId: String,
        query: String,
        topK: Int
    ): List<FtsSearchResult>
}

data class FtsSearchResult(
    val chunkId: String,
    val score: Float,
    val highlights: List<String> = emptyList()
)
```

### Metadata Filter

```kotlin
/**
 * Фильтры по метаданным
 */
sealed class MetadataFilter {
    data class Equals(val field: String, val value: Any) : MetadataFilter()
    data class In(val field: String, val values: List<Any>) : MetadataFilter()
    data class GreaterThan(val field: String, val value: Comparable<*>) : MetadataFilter()
    data class LessThan(val field: String, val value: Comparable<*>) : MetadataFilter()
    data class Between(
        val field: String, 
        val from: Comparable<*>, 
        val to: Comparable<*>
    ) : MetadataFilter()
    data class And(val filters: List<MetadataFilter>) : MetadataFilter()
    data class Or(val filters: List<MetadataFilter>) : MetadataFilter()
    data class Not(val filter: MetadataFilter) : MetadataFilter()
}
```

---

## Room Entities

### Collection

```kotlin
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val embeddingDimensions: Int,
    val distanceMetric: String,
    val documentCount: Int,
    val chunkCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Chunk

```kotlin
@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("collectionId"),
        Index("documentId"),
        Index("collectionId", "documentId")
    ]
)
data class ChunkEntity(
    @PrimaryKey
    val id: String,
    val collectionId: String,
    val documentId: String,
    val content: String,
    val chunkIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val level: String,
    val parentId: String?,
    val section: String?,
    val tokenCount: Int?,
    val embedding: String,      // FloatArray как CSV или Base64
    val metadataJson: String,
    val createdAt: Instant
)
```

### FTS Table

```kotlin
@Entity(tableName = "chunks_fts")
@Fts4(contentEntity = ChunkEntity::class)
data class ChunkFtsEntity(
    val content: String
)
```

---

## DAOs

```kotlin
@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?
    
    @Query("SELECT * FROM collections")
    suspend fun getAll(): List<CollectionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)
    
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("""
        UPDATE collections 
        SET documentCount = :count, updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateDocumentCount(id: String, count: Int, updatedAt: Instant)
    
    @Query("""
        UPDATE collections 
        SET chunkCount = :count, updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateChunkCount(id: String, count: Int, updatedAt: Instant)
}

@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE id = :id")
    suspend fun getById(id: String): ChunkEntity?
    
    @Query("""
        SELECT * FROM chunks 
        WHERE collectionId = :collectionId AND documentId = :documentId
    """)
    suspend fun getByDocument(collectionId: String, documentId: String): List<ChunkEntity>
    
    @Query("SELECT * FROM chunks WHERE collectionId = :collectionId")
    suspend fun getByCollection(collectionId: String): List<ChunkEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>)
    
    @Query("DELETE FROM chunks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
    
    @Query("""
        DELETE FROM chunks 
        WHERE collectionId = :collectionId AND documentId = :documentId
    """)
    suspend fun deleteByDocument(collectionId: String, documentId: String)
    
    @Query("SELECT COUNT(*) FROM chunks WHERE collectionId = :collectionId")
    suspend fun countByCollection(collectionId: String): Int
    
    @Query("SELECT DISTINCT documentId FROM chunks WHERE collectionId = :collectionId")
    suspend fun getDocumentIds(collectionId: String): List<String>
}

@Dao
interface ChunkFtsDao {
    @Query("SELECT rowid, * FROM chunks_fts WHERE chunks_fts MATCH :query")
    suspend fun search(query: String): List<ChunkFtsSearchResult>
}

data class ChunkFtsSearchResult(
    val rowid: Long,
    val content: String
)
```

---

## Room Vector Store

```kotlin
class RoomVectorStore(
    private val collectionDao: CollectionDao,
    private val chunkDao: ChunkDao,
    private val dispatchers: DispatcherProvider
) : VectorStore {
    
    override suspend fun createCollection(collection: Collection) {
        withContext(dispatchers.io) {
            collectionDao.insert(collection.toEntity())
        }
    }
    
    override suspend fun add(collectionId: String, chunks: List<EmbeddedChunk>) {
        withContext(dispatchers.io) {
            val entities = chunks.map { it.toEntity(collectionId) }
            chunkDao.insertAll(entities)
            updateCollectionStats(collectionId)
        }
    }
    
    override suspend fun searchSemantic(
        collectionId: String,
        query: Embedding,
        topK: Int,
        filter: MetadataFilter?
    ): List<SearchResult> {
        return withContext(dispatchers.default) {
            val allChunks = chunkDao.getByCollection(collectionId)
            
            allChunks
                .filter { filter?.matches(it.parseMetadata()) ?: true }
                .map { entity ->
                    val embedding = entity.parseEmbedding()
                    val score = cosineSimilarity(query.vector, embedding)
                    entity to score
                }
                .sortedByDescending { it.second }
                .take(topK)
                .map { (entity, score) ->
                    SearchResult(
                        chunk = entity.toChunk(),
                        score = score,
                        scoreBreakdown = ScoreBreakdown(semanticScore = score)
                    )
                }
        }
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have same dimensions" }
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }
    
    private suspend fun updateCollectionStats(collectionId: String) {
        val chunkCount = chunkDao.countByCollection(collectionId)
        val documentCount = chunkDao.getDocumentIds(collectionId).size
        val now = Clock.System.now()
        
        collectionDao.updateChunkCount(collectionId, chunkCount, now)
        collectionDao.updateDocumentCount(collectionId, documentCount, now)
    }
    
    // ... остальные методы
}
```

---

## Room FTS Store

```kotlin
class RoomFtsStore(
    private val chunkDao: ChunkDao,
    private val ftsDao: ChunkFtsDao,
    private val dispatchers: DispatcherProvider
) : FtsStore {
    
    override suspend fun search(
        collectionId: String,
        query: String,
        topK: Int
    ): List<FtsSearchResult> {
        return withContext(dispatchers.io) {
            // Преобразуем запрос в FTS формат
            val ftsQuery = query.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" OR ") { "$it*" }
            
            ftsDao.search(ftsQuery)
                .take(topK)
                .mapIndexed { index, result ->
                    FtsSearchResult(
                        chunkId = result.rowid.toString(),
                        score = 1f / (index + 1),
                        highlights = extractHighlights(result.content, query)
                    )
                }
        }
    }
    
    private fun extractHighlights(content: String, query: String): List<String> {
        val words = query.lowercase().split(" ")
        return content.split(".")
            .filter { sentence -> 
                words.any { sentence.lowercase().contains(it) } 
            }
            .take(3)
    }
    
    // ... остальные методы
}
```

---

## Document Tracking (Incremental Indexing)

```kotlin
@Entity(tableName = "document_tracking")
data class DocumentTrackingEntity(
    @PrimaryKey
    val documentId: String,
    val collectionId: String,
    val contentHash: String,
    val filePath: String?,
    val fileModifiedAt: Instant?,
    val indexedAt: Instant,
    val chunkCount: Int
)

@Dao
interface DocumentTrackingDao {
    @Query("""
        SELECT * FROM document_tracking 
        WHERE documentId = :documentId AND collectionId = :collectionId
    """)
    suspend fun get(documentId: String, collectionId: String): DocumentTrackingEntity?
    
    @Query("SELECT * FROM document_tracking WHERE collectionId = :collectionId")
    suspend fun getByCollection(collectionId: String): List<DocumentTrackingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentTrackingEntity)
    
    @Query("""
        DELETE FROM document_tracking 
        WHERE documentId = :documentId AND collectionId = :collectionId
    """)
    suspend fun delete(documentId: String, collectionId: String)
}

/**
 * Отслеживание изменений документов
 */
interface DocumentTracker {
    suspend fun isModified(document: Document, collectionId: String): Boolean
    suspend fun track(document: Document, collectionId: String, chunkCount: Int)
    suspend fun untrack(documentId: String, collectionId: String)
}

class RoomDocumentTracker(
    private val dao: DocumentTrackingDao
) : DocumentTracker {
    
    override suspend fun isModified(document: Document, collectionId: String): Boolean {
        val existing = dao.get(document.id, collectionId) 
            ?: return true  // Новый документ
        
        // Проверка по хэшу контента
        val currentHash = ContentHash.compute(document.content)
        if (existing.contentHash != currentHash) return true
        
        // Проверка даты модификации файла
        val source = document.source
        if (source is DocumentSource.File && existing.filePath == source.path) {
            val fileModified = getFileModificationTime(source.path)
            if (fileModified != null && existing.fileModifiedAt != null) {
                return fileModified > existing.fileModifiedAt
            }
        }
        
        return false
    }
    
    override suspend fun track(
        document: Document, 
        collectionId: String, 
        chunkCount: Int
    ) {
        val source = document.source
        
        dao.upsert(DocumentTrackingEntity(
            documentId = document.id,
            collectionId = collectionId,
            contentHash = ContentHash.compute(document.content),
            filePath = (source as? DocumentSource.File)?.path,
            fileModifiedAt = (source as? DocumentSource.File)?.let { 
                getFileModificationTime(it.path) 
            },
            indexedAt = Clock.System.now(),
            chunkCount = chunkCount
        ))
    }
    
    override suspend fun untrack(documentId: String, collectionId: String) {
        dao.delete(documentId, collectionId)
    }
    
    private fun getFileModificationTime(path: String): Instant? {
        // Platform-specific implementation
        return null
    }
}
```

---

## DI

```kotlin
val storageModule = module {
    // DAOs предоставляются из :core:database
    
    single<VectorStore> { 
        RoomVectorStore(get(), get(), get()) 
    }
    
    single<FtsStore> { 
        RoomFtsStore(get(), get(), get()) 
    }
    
    single<DocumentTracker> { 
        RoomDocumentTracker(get()) 
    }
}
```
