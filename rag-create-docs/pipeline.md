# :tool:rag:pipeline

Высокоуровневый API, объединяющий все компоненты RAG.

**Зависимости:** все RAG-модули, `:core:common`, `:core:network`, `:core:database`

---

## Конфигурация

```kotlin
data class RagPipelineConfig(
    val parsing: ParsingConfig = ParsingConfig(),
    val chunking: ChunkingConfig = ChunkingConfig(),
    val embedding: EmbeddingConfig,
    val retrieval: RetrievalConfig = RetrievalConfig(),
    val prompting: PromptConfig = PromptConfig()
)

data class ParsingConfig(
    val extractStructure: Boolean = true
)

data class PromptConfig(
    val template: String = DEFAULT_RAG_TEMPLATE,
    val maxContextTokens: Int = 4000,
    val includeMetadata: Boolean = true,
    val citationStyle: CitationStyle = CitationStyle.INLINE
)

enum class CitationStyle {
    NONE,
    INLINE,     // [1], [2], etc.
    FOOTNOTE    // Ссылки в конце
}

const val DEFAULT_RAG_TEMPLATE = """
Use the following context to answer the question. 
If the answer cannot be found in the context, say so.

Context:
{context}

Question: {question}

Answer:
""".trimIndent()
```

---

## RAG Pipeline Interface

```kotlin
/**
 * Высокоуровневый API для RAG
 */
interface RagPipeline {
    // Управление коллекциями
    suspend fun createCollection(name: String, description: String? = null): Collection
    suspend fun deleteCollection(collectionId: String)
    suspend fun getCollection(collectionId: String): Collection?
    suspend fun listCollections(): List<Collection>
    
    // Индексация
    suspend fun index(collectionId: String, document: Document): IndexResult
    suspend fun index(collectionId: String, documents: List<Document>): List<IndexResult>
    suspend fun indexFile(collectionId: String, filePath: String): IndexResult
    suspend fun reindex(collectionId: String, documentId: String): IndexResult
    suspend fun removeDocument(collectionId: String, documentId: String)
    
    // Поиск
    suspend fun query(collectionId: String, question: String): RagResult
    suspend fun query(
        collectionId: String, 
        question: String, 
        config: RetrievalConfig
    ): RagResult
    
    // Низкоуровневый доступ
    val parser: ParserRegistry
    val chunker: Chunker
    val embedder: Embedder
    val retriever: Retriever
    val vectorStore: VectorStore
}
```

---

## Result Types

```kotlin
data class IndexResult(
    val documentId: String,
    val chunkCount: Int,
    val status: IndexStatus,
    val error: String? = null
)

enum class IndexStatus {
    INDEXED,
    UPDATED,
    SKIPPED,  // Не изменился (incremental indexing)
    FAILED
}

data class RagResult(
    val chunks: List<SearchResult>,
    val context: String,
    val augmentedPrompt: String,
    val metadata: RagResultMetadata
)

data class RagResultMetadata(
    val totalChunks: Int,
    val contextTokens: Int,
    val retrievalTimeMs: Long,
    val sources: List<SourceReference>
)

data class SourceReference(
    val documentId: String,
    val title: String?,
    val section: String?,
    val relevanceScore: Float
)
```

---

## Builder

```kotlin
class RagPipelineBuilder {
    private var httpClient: HttpClient? = null
    private var database: RagDatabase? = null
    private var dispatchers: DispatcherProvider = DefaultDispatcherProvider()
    
    // Components
    private var parserRegistry: ParserRegistry? = null
    private var chunker: Chunker? = null
    private var embedder: Embedder? = null
    private var retriever: Retriever? = null
    private var vectorStore: VectorStore? = null
    private var ftsStore: FtsStore? = null
    private var documentTracker: DocumentTracker? = null
    private var tokenizer: Tokenizer = SimpleTokenizer()
    
    // Config
    private var config: RagPipelineConfig? = null
    
    fun httpClient(client: HttpClient) = apply { this.httpClient = client }
    fun database(db: RagDatabase) = apply { this.database = db }
    fun dispatchers(dispatchers: DispatcherProvider) = apply { 
        this.dispatchers = dispatchers 
    }
    
    // Chunking
    fun chunking(block: ChunkerBuilder.() -> Unit) = apply {
        this.chunker = ChunkerBuilder().apply(block).build()
    }
    
    fun chunker(chunker: Chunker) = apply { this.chunker = chunker }
    
    // Embedding
    fun embedding(block: EmbedderBuilder.() -> Unit) = apply {
        this.embedder = EmbedderBuilder()
            .apply { httpClient?.let { httpClient(it) } }
            .apply(block)
            .build()
    }
    
    fun embedder(embedder: Embedder) = apply { this.embedder = embedder }
    
    // Retrieval
    fun retrieval(block: RetrieverBuilder.() -> Unit) = apply {
        this.retriever = RetrieverBuilder()
            .apply {
                vectorStore?.let { vectorStore(it) }
                ftsStore?.let { ftsStore(it) }
                embedder?.let { embedder(it) }
            }
            .apply(block)
            .build()
    }
    
    fun retriever(retriever: Retriever) = apply { this.retriever = retriever }
    
    // Config
    fun config(config: RagPipelineConfig) = apply { this.config = config }
    
    fun config(block: RagPipelineConfigBuilder.() -> Unit) = apply {
        this.config = RagPipelineConfigBuilder().apply(block).build()
    }
    
    fun build(): RagPipeline {
        val db = database ?: error("Database required")
        val embeddingConfig = config?.embedding ?: error("Embedding config required")
        
        val actualParser = parserRegistry ?: ParserRegistry().apply {
            register(MarkdownParser())
        }
        
        val actualChunker = chunker ?: chunker {
            sentenceBased()
            config {
                maxChunkSize = 512
                overlap = 50
            }
        }
        
        val actualEmbedder = embedder ?: embedder {
            httpClient(httpClient ?: error("HttpClient required"))
            ollama(embeddingConfig)
            cached()
        }
        
        val actualVectorStore = vectorStore ?: RoomVectorStore(
            db.collectionDao(),
            db.chunkDao(),
            dispatchers
        )
        
        val actualFtsStore = ftsStore ?: RoomFtsStore(
            db.chunkDao(),
            db.chunkFtsDao(),
            dispatchers
        )
        
        val actualDocTracker = documentTracker ?: RoomDocumentTracker(
            db.documentTrackingDao()
        )
        
        val actualRetriever = retriever ?: DefaultRetriever(
            vectorStore = actualVectorStore,
            ftsStore = actualFtsStore,
            embedder = actualEmbedder,
            reranker = SimpleReranker()
        )
        
        return DefaultRagPipeline(
            parser = actualParser,
            chunker = actualChunker,
            embedder = actualEmbedder,
            retriever = actualRetriever,
            vectorStore = actualVectorStore,
            ftsStore = actualFtsStore,
            documentTracker = actualDocTracker,
            config = config ?: RagPipelineConfig(embedding = embeddingConfig),
            tokenizer = tokenizer,
            dispatchers = dispatchers
        )
    }
}

class RagPipelineConfigBuilder {
    var parsing: ParsingConfig = ParsingConfig()
    var chunking: ChunkingConfig = ChunkingConfig()
    var embedding: EmbeddingConfig? = null
    var retrieval: RetrievalConfig = RetrievalConfig()
    var prompting: PromptConfig = PromptConfig()
    
    fun embedding(
        baseUrl: String,
        model: String,
        dimensions: Int,
        apiKey: String? = null
    ) = apply {
        embedding = EmbeddingConfig(
            baseUrl = baseUrl,
            model = model,
            dimensions = dimensions,
            apiKey = apiKey
        )
    }
    
    fun chunking(block: ChunkingConfigBuilder.() -> Unit) = apply {
        chunking = ChunkingConfigBuilder().apply(block).build()
    }
    
    fun retrieval(block: RetrievalConfig.() -> Unit) = apply {
        retrieval = RetrievalConfig().copy().apply(block)
    }
    
    fun prompting(block: PromptConfig.() -> Unit) = apply {
        prompting = PromptConfig().copy().apply(block)
    }
    
    fun build(): RagPipelineConfig {
        return RagPipelineConfig(
            parsing = parsing,
            chunking = chunking,
            embedding = embedding ?: error("Embedding config required"),
            retrieval = retrieval,
            prompting = prompting
        )
    }
}

// DSL entry point
fun ragPipeline(block: RagPipelineBuilder.() -> Unit): RagPipeline {
    return RagPipelineBuilder().apply(block).build()
}
```

---

## Использование

### Полная конфигурация

```kotlin
val pipeline = ragPipeline {
    httpClient(httpClient)
    database(ragDatabase)
    
    config {
        embedding(
            baseUrl = "http://localhost:11434",
            model = "nomic-embed-text",
            dimensions = 768
        )
        
        chunking {
            maxChunkSize = 512
            overlap = 50
        }
        
        retrieval {
            topK = 5
            mode = RetrievalMode.HYBRID
        }
        
        prompting {
            maxContextTokens = 4000
            citationStyle = CitationStyle.INLINE
        }
    }
    
    chunking {
        parentChild()
    }
    
    embedding {
        cached()
    }
}
```

### Создание коллекции и индексация

```kotlin
// Создаём коллекцию
val collection = pipeline.createCollection(
    name = "documentation",
    description = "Project documentation"
)

// Индексируем файл
val result = pipeline.indexFile(collection.id, "/path/to/README.md")
println("Indexed ${result.chunkCount} chunks")

// Индексируем несколько документов
val documents = listOf(
    Document(id = "doc1", content = "..."),
    Document(id = "doc2", content = "...")
)
val results = pipeline.index(collection.id, documents)
```

### Поиск

```kotlin
// Простой поиск
val result = pipeline.query(collection.id, "How to configure logging?")

println("Found ${result.chunks.size} relevant chunks")
println("Context tokens: ${result.metadata.contextTokens}")
println("Retrieval time: ${result.metadata.retrievalTimeMs}ms")

// Использование augmented prompt с LLM
val llmResponse = llmClient.chat(ChatRequest(
    messages = listOf(Message.User(result.augmentedPrompt))
))

// Поиск с кастомной конфигурацией
val customResult = pipeline.query(
    collectionId = collection.id,
    question = "API reference",
    config = RetrievalConfig(
        mode = RetrievalMode.HYBRID,
        topK = 10,
        filter = MetadataFilter.Equals("type", "api-docs")
    )
)
```

### Доступ к компонентам

```kotlin
// Низкоуровневый доступ
val chunks = pipeline.chunker.chunk("Some long text...")
val embeddings = pipeline.embedder.embed(chunks)

val searchResults = pipeline.retriever.retrieve(
    collectionId = collection.id,
    query = "search query",
    config = RetrievalConfig(mode = RetrievalMode.SEMANTIC)
)
```

---

## DI

```kotlin
val pipelineModule = module {
    single<RagPipeline> { params ->
        val config: RagPipelineConfig = params.get()
        
        ragPipeline {
            httpClient(get())
            database(get())
            dispatchers(get())
            config(config)
        }
    }
}

// Общий модуль для подключения всего RAG
val ragModules = listOf(
    parserModule,
    chunkingModule,
    embeddingModule,
    storageModule,
    retrievalModule,
    pipelineModule
)
```

---

## Интеграция с Chat Feature

```kotlin
// В :feature:chat:impl

class ChatRepositoryImpl(
    private val llmClient: LlmClient,
    private val ragPipeline: RagPipeline?,  // Опционально
    private val config: ChatConfig,
    // ...
) : ChatRepository {
    
    override fun sendMessage(
        conversationId: String,
        content: String
    ): Flow<ChatMessage> = flow {
        // RAG augmentation
        val finalContent = if (ragPipeline != null && config.ragEnabled) {
            val ragResult = ragPipeline.query(config.ragCollectionId, content)
            ragResult.augmentedPrompt
        } else {
            content
        }
        
        // LLM request
        val request = ChatRequest(
            messages = buildMessages(conversationId, finalContent)
        )
        
        llmClient.chat(request).collect { chunk ->
            // Stream response
        }
    }
}
```
