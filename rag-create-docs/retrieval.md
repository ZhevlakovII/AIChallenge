# :tool:rag:retrieval

Поиск с поддержкой semantic, hybrid и reranking.

**Зависимости:** `:tool:rag:core`, `:tool:rag:storage`, `:tool:rag:embedding`, `:core:common`, `:core:network`

---

## Конфигурация

```kotlin
data class RetrievalConfig(
    val topK: Int = 10,
    val mode: RetrievalMode = RetrievalMode.HYBRID,
    val hybridConfig: HybridConfig = HybridConfig(),
    val rerankConfig: RerankConfig? = null,
    val filter: MetadataFilter? = null,
    val includeParentContext: Boolean = true  // Для parent-child
)

enum class RetrievalMode {
    SEMANTIC,   // Только по эмбеддингам
    KEYWORD,    // Только FTS
    HYBRID      // Комбинация
}

data class HybridConfig(
    val semanticWeight: Float = 0.7f,
    val keywordWeight: Float = 0.3f,
    val fusionMethod: FusionMethod = FusionMethod.RRF
)

enum class FusionMethod {
    RRF,        // Reciprocal Rank Fusion
    WEIGHTED    // Weighted combination
}

data class RerankConfig(
    val enabled: Boolean = true,
    val topK: Int = 20,  // Сколько результатов передать в reranker
    val provider: RerankProvider = RerankProvider.LOCAL
)

enum class RerankProvider {
    LOCAL,      // Простой reranker
    API         // Внешний API
}
```

---

## Retriever

```kotlin
/**
 * Главный интерфейс для поиска
 */
interface Retriever {
    suspend fun retrieve(
        collectionId: String,
        query: String,
        config: RetrievalConfig = RetrievalConfig()
    ): List<SearchResult>
}

class DefaultRetriever(
    private val vectorStore: VectorStore,
    private val ftsStore: FtsStore,
    private val embedder: Embedder,
    private val reranker: Reranker?
) : Retriever {
    
    override suspend fun retrieve(
        collectionId: String,
        query: String,
        config: RetrievalConfig
    ): List<SearchResult> {
        // 1. Получаем кандидатов
        val candidates = when (config.mode) {
            RetrievalMode.SEMANTIC -> retrieveSemantic(collectionId, query, config)
            RetrievalMode.KEYWORD -> retrieveKeyword(collectionId, query, config)
            RetrievalMode.HYBRID -> retrieveHybrid(collectionId, query, config)
        }
        
        // 2. Reranking
        val reranked = if (config.rerankConfig?.enabled == true && reranker != null) {
            reranker.rerank(query, candidates, config.rerankConfig.topK)
        } else {
            candidates
        }
        
        // 3. Parent context
        val withContext = if (config.includeParentContext) {
            enrichWithParentContext(collectionId, reranked)
        } else {
            reranked
        }
        
        return withContext.take(config.topK)
    }
    
    private suspend fun retrieveSemantic(
        collectionId: String,
        query: String,
        config: RetrievalConfig
    ): List<SearchResult> {
        val queryEmbedding = embedder.embed(query)
        val k = config.rerankConfig?.topK ?: config.topK
        
        return vectorStore.searchSemantic(
            collectionId = collectionId,
            query = queryEmbedding,
            topK = k,
            filter = config.filter
        )
    }
    
    private suspend fun retrieveKeyword(
        collectionId: String,
        query: String,
        config: RetrievalConfig
    ): List<SearchResult> {
        val k = config.rerankConfig?.topK ?: config.topK
        val ftsResults = ftsStore.search(collectionId, query, k)
        
        return ftsResults.mapNotNull { fts ->
            vectorStore.get(collectionId, fts.chunkId)?.let { embedded ->
                SearchResult(
                    chunk = embedded.chunk,
                    score = fts.score,
                    scoreBreakdown = ScoreBreakdown(keywordScore = fts.score)
                )
            }
        }
    }
    
    private suspend fun retrieveHybrid(
        collectionId: String,
        query: String,
        config: RetrievalConfig
    ): List<SearchResult> {
        val hybridConfig = config.hybridConfig
        val k = config.rerankConfig?.topK ?: config.topK
        
        val semanticResults = retrieveSemantic(collectionId, query, config.copy(topK = k))
        val keywordResults = retrieveKeyword(collectionId, query, config.copy(topK = k))
        
        return when (hybridConfig.fusionMethod) {
            FusionMethod.RRF -> fusionRRF(semanticResults, keywordResults, k)
            FusionMethod.WEIGHTED -> fusionWeighted(
                semanticResults, 
                keywordResults, 
                hybridConfig.semanticWeight,
                hybridConfig.keywordWeight,
                k
            )
        }
    }
}
```

---

## Fusion Methods

### Reciprocal Rank Fusion (RRF)

```kotlin
/**
 * RRF: score = Σ 1 / (k + rank)
 */
private fun fusionRRF(
    semantic: List<SearchResult>,
    keyword: List<SearchResult>,
    topK: Int,
    k: Int = 60  // RRF constant
): List<SearchResult> {
    val scores = mutableMapOf<String, Float>()
    val chunks = mutableMapOf<String, Chunk>()
    val breakdowns = mutableMapOf<String, MutableMap<String, Float>>()
    
    // Semantic scores
    semantic.forEachIndexed { rank, result ->
        val chunkId = result.chunk.id
        val rrfScore = 1f / (k + rank + 1)
        scores[chunkId] = (scores[chunkId] ?: 0f) + rrfScore
        chunks[chunkId] = result.chunk
        breakdowns.getOrPut(chunkId) { mutableMapOf() }["semantic"] = result.score
    }
    
    // Keyword scores
    keyword.forEachIndexed { rank, result ->
        val chunkId = result.chunk.id
        val rrfScore = 1f / (k + rank + 1)
        scores[chunkId] = (scores[chunkId] ?: 0f) + rrfScore
        chunks[chunkId] = result.chunk
        breakdowns.getOrPut(chunkId) { mutableMapOf() }["keyword"] = result.score
    }
    
    return scores.entries
        .sortedByDescending { it.value }
        .take(topK)
        .map { (chunkId, score) ->
            val bd = breakdowns[chunkId]!!
            SearchResult(
                chunk = chunks[chunkId]!!,
                score = score,
                scoreBreakdown = ScoreBreakdown(
                    semanticScore = bd["semantic"],
                    keywordScore = bd["keyword"]
                )
            )
        }
}
```

### Weighted Fusion

```kotlin
private fun fusionWeighted(
    semantic: List<SearchResult>,
    keyword: List<SearchResult>,
    semanticWeight: Float,
    keywordWeight: Float,
    topK: Int
): List<SearchResult> {
    val scores = mutableMapOf<String, Float>()
    val chunks = mutableMapOf<String, Chunk>()
    val breakdowns = mutableMapOf<String, MutableMap<String, Float>>()
    
    semantic.forEach { result ->
        val chunkId = result.chunk.id
        scores[chunkId] = (scores[chunkId] ?: 0f) + result.score * semanticWeight
        chunks[chunkId] = result.chunk
        breakdowns.getOrPut(chunkId) { mutableMapOf() }["semantic"] = result.score
    }
    
    keyword.forEach { result ->
        val chunkId = result.chunk.id
        scores[chunkId] = (scores[chunkId] ?: 0f) + result.score * keywordWeight
        chunks[chunkId] = result.chunk
        breakdowns.getOrPut(chunkId) { mutableMapOf() }["keyword"] = result.score
    }
    
    return scores.entries
        .sortedByDescending { it.value }
        .take(topK)
        .map { (chunkId, score) ->
            val bd = breakdowns[chunkId]!!
            SearchResult(
                chunk = chunks[chunkId]!!,
                score = score,
                scoreBreakdown = ScoreBreakdown(
                    semanticScore = bd["semantic"],
                    keywordScore = bd["keyword"]
                )
            )
        }
}
```

---

## Parent Context Enrichment

```kotlin
/**
 * Добавляет parent контекст для child чанков
 */
private suspend fun enrichWithParentContext(
    collectionId: String,
    results: List<SearchResult>
): List<SearchResult> {
    return results.map { result ->
        val parentId = result.chunk.metadata.parentId
        
        if (parentId != null && result.chunk.metadata.level == ChunkLevel.CHILD) {
            val parent = vectorStore.get(collectionId, parentId)
            if (parent != null) {
                result.copy(
                    chunk = result.chunk.copy(
                        metadata = result.chunk.metadata.copy(
                            section = parent.chunk.content.take(200)
                        )
                    )
                )
            } else result
        } else result
    }
}
```

---

## Reranking

### Абстракция

```kotlin
/**
 * Reranker переоценивает релевантность результатов
 */
interface Reranker {
    suspend fun rerank(
        query: String,
        results: List<SearchResult>,
        topK: Int
    ): List<SearchResult>
}
```

### Simple Reranker (локальный)

```kotlin
/**
 * Простой reranker без внешних зависимостей
 * Использует overlap слов между query и chunk
 */
class SimpleReranker : Reranker {
    
    override suspend fun rerank(
        query: String,
        results: List<SearchResult>,
        topK: Int
    ): List<SearchResult> {
        val queryWords = query.lowercase().split(" ").toSet()
        
        return results
            .map { result ->
                val chunkWords = result.chunk.content.lowercase().split(" ").toSet()
                val overlap = queryWords.intersect(chunkWords).size.toFloat()
                val rerankScore = overlap / queryWords.size
                
                result.copy(
                    scoreBreakdown = result.scoreBreakdown?.copy(
                        rerankScore = rerankScore
                    ) ?: ScoreBreakdown(rerankScore = rerankScore)
                )
            }
            .sortedByDescending { it.scoreBreakdown?.rerankScore ?: 0f }
            .take(topK)
    }
}
```

### API Reranker (внешний)

```kotlin
/**
 * Cross-encoder reranker через API
 */
class ApiReranker(
    private val httpClient: HttpClient,
    private val config: ApiRerankerConfig
) : Reranker {
    
    override suspend fun rerank(
        query: String,
        results: List<SearchResult>,
        topK: Int
    ): List<SearchResult> {
        if (results.isEmpty()) return emptyList()
        
        val request = RerankRequest(
            model = config.model,
            query = query,
            documents = results.map { it.chunk.content },
            topN = topK
        )
        
        val response = httpClient.post("${config.baseUrl}/rerank") {
            config.apiKey?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        val rerankResponse = response.body<RerankResponse>()
        
        return rerankResponse.results
            .sortedByDescending { it.relevanceScore }
            .take(topK)
            .map { reranked ->
                val original = results[reranked.index]
                original.copy(
                    score = reranked.relevanceScore,
                    scoreBreakdown = original.scoreBreakdown?.copy(
                        rerankScore = reranked.relevanceScore
                    ) ?: ScoreBreakdown(rerankScore = reranked.relevanceScore)
                )
            }
    }
}

data class ApiRerankerConfig(
    val baseUrl: String,
    val apiKey: String?,
    val model: String
)

@Serializable
private data class RerankRequest(
    val model: String,
    val query: String,
    val documents: List<String>,
    @SerialName("top_n")
    val topN: Int
)

@Serializable
private data class RerankResponse(
    val results: List<RerankResult>
)

@Serializable
private data class RerankResult(
    val index: Int,
    @SerialName("relevance_score")
    val relevanceScore: Float
)
```

---

## Builder

```kotlin
class RetrieverBuilder {
    private var vectorStore: VectorStore? = null
    private var ftsStore: FtsStore? = null
    private var embedder: Embedder? = null
    private var reranker: Reranker? = null
    
    fun vectorStore(store: VectorStore) = apply { this.vectorStore = store }
    fun ftsStore(store: FtsStore) = apply { this.ftsStore = store }
    fun embedder(embedder: Embedder) = apply { this.embedder = embedder }
    
    fun simpleReranker() = apply { this.reranker = SimpleReranker() }
    
    fun apiReranker(config: ApiRerankerConfig, httpClient: HttpClient) = apply {
        this.reranker = ApiReranker(httpClient, config)
    }
    
    fun reranker(reranker: Reranker) = apply { this.reranker = reranker }
    
    fun build(): Retriever {
        return DefaultRetriever(
            vectorStore = vectorStore ?: error("VectorStore required"),
            ftsStore = ftsStore ?: error("FtsStore required"),
            embedder = embedder ?: error("Embedder required"),
            reranker = reranker
        )
    }
}

// DSL entry point
fun retriever(block: RetrieverBuilder.() -> Unit): Retriever {
    return RetrieverBuilder().apply(block).build()
}
```

---

## Использование

```kotlin
val retriever = retriever {
    vectorStore(vectorStore)
    ftsStore(ftsStore)
    embedder(embedder)
    simpleReranker()
}

// Semantic search
val semanticResults = retriever.retrieve(
    collectionId = "my-collection",
    query = "How to configure logging?",
    config = RetrievalConfig(
        mode = RetrievalMode.SEMANTIC,
        topK = 5
    )
)

// Hybrid search with RRF
val hybridResults = retriever.retrieve(
    collectionId = "my-collection",
    query = "logging configuration",
    config = RetrievalConfig(
        mode = RetrievalMode.HYBRID,
        topK = 10,
        hybridConfig = HybridConfig(
            semanticWeight = 0.7f,
            keywordWeight = 0.3f,
            fusionMethod = FusionMethod.RRF
        ),
        rerankConfig = RerankConfig(
            enabled = true,
            topK = 20
        )
    )
)

// With metadata filter
val filteredResults = retriever.retrieve(
    collectionId = "my-collection",
    query = "API reference",
    config = RetrievalConfig(
        filter = MetadataFilter.And(listOf(
            MetadataFilter.Equals("type", "documentation"),
            MetadataFilter.GreaterThan("date", "2024-01-01")
        ))
    )
)
```

---

## DI

```kotlin
val retrievalModule = module {
    factory<Reranker> { (config: RerankConfig?) ->
        when (config?.provider) {
            RerankProvider.API -> ApiReranker(get(), get())
            RerankProvider.LOCAL, null -> SimpleReranker()
        }
    }
    
    single<Retriever> {
        DefaultRetriever(
            vectorStore = get(),
            ftsStore = get(),
            embedder = get(),
            reranker = getOrNull()
        )
    }
}
```
