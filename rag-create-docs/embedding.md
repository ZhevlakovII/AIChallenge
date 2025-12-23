# :tool:rag:embedding

Генерация эмбеддингов через внешний API.

**Зависимости:** `:tool:rag:core`, `:core:common`, `:core:network`, `:core:database`

---

## Абстракция

```kotlin
/**
 * Провайдер эмбеддингов
 */
interface EmbeddingProvider {
    val modelId: String
    val dimensions: Int
    
    suspend fun embed(text: String): Embedding
    suspend fun embedBatch(texts: List<String>): List<Embedding>
}

/**
 * Конфигурация провайдера
 */
data class EmbeddingConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
    val dimensions: Int,
    val maxBatchSize: Int = 100,
    val timeout: Duration = 30.seconds
)
```

---

## Реализации

### Ollama

```kotlin
class OllamaEmbeddingProvider(
    private val httpClient: HttpClient,
    private val config: EmbeddingConfig
) : EmbeddingProvider {
    
    override val modelId: String = config.model
    override val dimensions: Int = config.dimensions
    
    override suspend fun embed(text: String): Embedding {
        val response = httpClient.post("${config.baseUrl}/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(OllamaEmbeddingRequest(model = config.model, prompt = text))
        }
        
        val result = response.body<OllamaEmbeddingResponse>()
        return Embedding(result.embedding.toFloatArray())
    }
    
    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        // Ollama не поддерживает batch — параллельные запросы
        return coroutineScope {
            texts.map { text ->
                async { embed(text) }
            }.awaitAll()
        }
    }
}

@Serializable
private data class OllamaEmbeddingRequest(
    val model: String,
    val prompt: String
)

@Serializable
private data class OllamaEmbeddingResponse(
    val embedding: List<Float>
)
```

### OpenAI-совместимый

```kotlin
class OpenAiEmbeddingProvider(
    private val httpClient: HttpClient,
    private val config: EmbeddingConfig
) : EmbeddingProvider {
    
    override val modelId: String = config.model
    override val dimensions: Int = config.dimensions
    
    override suspend fun embed(text: String): Embedding {
        return embedBatch(listOf(text)).first()
    }
    
    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        val response = httpClient.post("${config.baseUrl}/embeddings") {
            config.apiKey?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            setBody(OpenAiEmbeddingRequest(model = config.model, input = texts))
        }
        
        val result = response.body<OpenAiEmbeddingResponse>()
        return result.data
            .sortedBy { it.index }
            .map { Embedding(it.embedding.toFloatArray()) }
    }
}

@Serializable
private data class OpenAiEmbeddingRequest(
    val model: String,
    val input: List<String>
)

@Serializable
private data class OpenAiEmbeddingResponse(
    val data: List<EmbeddingData>
)

@Serializable
private data class EmbeddingData(
    val index: Int,
    val embedding: List<Float>
)
```

---

## Кэширование

### Абстракция кэша

```kotlin
/**
 * Кэш эмбеддингов по хэшу контента
 */
interface EmbeddingCache {
    suspend fun get(contentHash: String, modelId: String): Embedding?
    suspend fun put(contentHash: String, modelId: String, embedding: Embedding)
    suspend fun invalidate(contentHash: String)
    suspend fun clear()
}
```

### In-Memory кэш

```kotlin
class InMemoryEmbeddingCache : EmbeddingCache {
    private val cache = ConcurrentHashMap<String, Embedding>()
    
    private fun key(contentHash: String, modelId: String) = "$modelId:$contentHash"
    
    override suspend fun get(contentHash: String, modelId: String): Embedding? {
        return cache[key(contentHash, modelId)]
    }
    
    override suspend fun put(contentHash: String, modelId: String, embedding: Embedding) {
        cache[key(contentHash, modelId)] = embedding
    }
    
    override suspend fun invalidate(contentHash: String) {
        cache.keys
            .filter { it.endsWith(":$contentHash") }
            .forEach { cache.remove(it) }
    }
    
    override suspend fun clear() {
        cache.clear()
    }
}
```

### Room кэш

```kotlin
class RoomEmbeddingCache(
    private val dao: EmbeddingCacheDao
) : EmbeddingCache {
    
    override suspend fun get(contentHash: String, modelId: String): Embedding? {
        return dao.get(contentHash, modelId)?.let { 
            Embedding(it.embedding.toFloatArray()) 
        }
    }
    
    override suspend fun put(contentHash: String, modelId: String, embedding: Embedding) {
        dao.insert(EmbeddingCacheEntity(
            contentHash = contentHash,
            modelId = modelId,
            embedding = embedding.vector.toList(),
            createdAt = Clock.System.now()
        ))
    }
    
    override suspend fun invalidate(contentHash: String) {
        dao.deleteByContentHash(contentHash)
    }
    
    override suspend fun clear() {
        dao.deleteAll()
    }
}

@Entity(tableName = "embedding_cache")
data class EmbeddingCacheEntity(
    @PrimaryKey
    val id: String = "${contentHash}_$modelId",
    val contentHash: String,
    val modelId: String,
    val embedding: List<Float>,
    val createdAt: Instant
)

@Dao
interface EmbeddingCacheDao {
    @Query("SELECT * FROM embedding_cache WHERE contentHash = :contentHash AND modelId = :modelId")
    suspend fun get(contentHash: String, modelId: String): EmbeddingCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmbeddingCacheEntity)
    
    @Query("DELETE FROM embedding_cache WHERE contentHash = :contentHash")
    suspend fun deleteByContentHash(contentHash: String)
    
    @Query("DELETE FROM embedding_cache")
    suspend fun deleteAll()
}
```

### Cached Provider

```kotlin
/**
 * Провайдер с кэшированием
 */
class CachedEmbeddingProvider(
    private val delegate: EmbeddingProvider,
    private val cache: EmbeddingCache
) : EmbeddingProvider by delegate {
    
    override suspend fun embed(text: String): Embedding {
        val hash = ContentHash.compute(text)
        
        cache.get(hash, modelId)?.let { return it }
        
        val embedding = delegate.embed(text)
        cache.put(hash, modelId, embedding)
        
        return embedding
    }
    
    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        val hashes = texts.map { ContentHash.compute(it) }
        val results = arrayOfNulls<Embedding>(texts.size)
        val toCompute = mutableListOf<Pair<Int, String>>()
        
        // Проверяем кэш
        hashes.forEachIndexed { index, hash ->
            val cached = cache.get(hash, modelId)
            if (cached != null) {
                results[index] = cached
            } else {
                toCompute.add(index to texts[index])
            }
        }
        
        // Вычисляем недостающие
        if (toCompute.isNotEmpty()) {
            val computed = delegate.embedBatch(toCompute.map { it.second })
            toCompute.zip(computed).forEach { (indexedText, embedding) ->
                val (index, _) = indexedText
                results[index] = embedding
                cache.put(hashes[index], modelId, embedding)
            }
        }
        
        return results.map { it!! }
    }
}
```

---

## Embedder Facade

```kotlin
/**
 * Главный интерфейс для работы с эмбеддингами
 */
interface Embedder {
    val dimensions: Int
    
    suspend fun embed(text: String): Embedding
    suspend fun embed(texts: List<String>): List<Embedding>
    suspend fun embed(chunk: Chunk): EmbeddedChunk
    suspend fun embed(chunks: List<Chunk>): List<EmbeddedChunk>
}

class DefaultEmbedder(
    private val provider: EmbeddingProvider
) : Embedder {
    
    override val dimensions: Int = provider.dimensions
    
    override suspend fun embed(text: String): Embedding {
        return provider.embed(text)
    }
    
    override suspend fun embed(texts: List<String>): List<Embedding> {
        return provider.embedBatch(texts)
    }
    
    override suspend fun embed(chunk: Chunk): EmbeddedChunk {
        val embedding = provider.embed(chunk.content)
        return EmbeddedChunk(chunk, embedding)
    }
    
    override suspend fun embed(chunks: List<Chunk>): List<EmbeddedChunk> {
        val embeddings = provider.embedBatch(chunks.map { it.content })
        return chunks.zip(embeddings).map { (chunk, embedding) ->
            EmbeddedChunk(chunk, embedding)
        }
    }
}
```

---

## Builder

```kotlin
class EmbedderBuilder {
    private var provider: EmbeddingProvider? = null
    private var cache: EmbeddingCache? = null
    private var httpClient: HttpClient? = null
    
    fun httpClient(client: HttpClient) = apply { this.httpClient = client }
    
    fun ollama(config: EmbeddingConfig) = apply {
        val client = httpClient ?: error("HttpClient required")
        provider = OllamaEmbeddingProvider(client, config)
    }
    
    fun ollama(baseUrl: String, model: String, dimensions: Int) = apply {
        ollama(EmbeddingConfig(
            baseUrl = baseUrl, 
            model = model, 
            dimensions = dimensions
        ))
    }
    
    fun openAi(config: EmbeddingConfig) = apply {
        val client = httpClient ?: error("HttpClient required")
        provider = OpenAiEmbeddingProvider(client, config)
    }
    
    fun provider(provider: EmbeddingProvider) = apply {
        this.provider = provider
    }
    
    fun cached(cache: EmbeddingCache = InMemoryEmbeddingCache()) = apply {
        this.cache = cache
    }
    
    fun build(): Embedder {
        var p = provider ?: error("Embedding provider required")
        
        cache?.let { c ->
            p = CachedEmbeddingProvider(p, c)
        }
        
        return DefaultEmbedder(p)
    }
}

// DSL entry point
fun embedder(block: EmbedderBuilder.() -> Unit): Embedder {
    return EmbedderBuilder().apply(block).build()
}
```

---

## Использование

```kotlin
// Ollama с кэшированием
val embedder = embedder {
    httpClient(client)
    ollama(
        baseUrl = "http://localhost:11434",
        model = "nomic-embed-text",
        dimensions = 768
    )
    cached()
}

val embedding = embedder.embed("Some text to embed")

// Batch embedding
val embeddings = embedder.embed(listOf("Text 1", "Text 2", "Text 3"))

// Embed chunks
val embeddedChunks = embedder.embed(chunks)
```

---

## DI

```kotlin
val embeddingModule = module {
    // Cache
    single<EmbeddingCache> { RoomEmbeddingCache(get()) }
    
    // Provider factory
    factory<EmbeddingProvider> { (config: EmbeddingConfig) ->
        val provider = when {
            config.baseUrl.contains("ollama") || 
            config.baseUrl.contains("11434") -> 
                OllamaEmbeddingProvider(get(), config)
            else -> 
                OpenAiEmbeddingProvider(get(), config)
        }
        CachedEmbeddingProvider(provider, get())
    }
    
    // Embedder
    factory { (config: EmbeddingConfig) ->
        DefaultEmbedder(get { parametersOf(config) })
    }
}
```
