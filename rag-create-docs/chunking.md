# :tool:rag:chunking

Стратегии разбиения текста на чанки.

**Зависимости:** `:tool:rag:core`, `:core:common`

---

## Абстракция

```kotlin
/**
 * Стратегия чанкования
 */
interface ChunkingStrategy {
    val name: String
    
    fun chunk(document: Document, config: ChunkingConfig): List<Chunk>
}

data class ChunkingConfig(
    val maxChunkSize: Int = 512,          // В токенах или символах
    val minChunkSize: Int = 100,
    val overlap: Int = 50,
    val sizeUnit: SizeUnit = SizeUnit.TOKENS,
    val preserveSentences: Boolean = true  // Не разрывать предложения
)

enum class SizeUnit {
    TOKENS,
    CHARACTERS
}
```

---

## Токенизатор

```kotlin
/**
 * Токенизатор для подсчёта токенов
 */
interface Tokenizer {
    fun tokenize(text: String): List<String>
    fun countTokens(text: String): Int = tokenize(text).size
    fun truncate(text: String, maxTokens: Int): String
}

/**
 * Простой токенизатор на основе пробелов и пунктуации
 */
class SimpleTokenizer : Tokenizer {
    private val tokenRegex = Regex("\\s+|(?<=[.,!?;:])|(?=[.,!?;:])")
    
    override fun tokenize(text: String): List<String> {
        return text.split(tokenRegex).filter { it.isNotBlank() }
    }
    
    override fun truncate(text: String, maxTokens: Int): String {
        val tokens = tokenize(text)
        if (tokens.size <= maxTokens) return text
        return tokens.take(maxTokens).joinToString(" ")
    }
}

/**
 * Tiktoken-совместимый токенизатор (для точного подсчёта)
 */
expect class TiktokenTokenizer(model: String = "cl100k_base") : Tokenizer
```

---

## Стратегии чанкования

### Token-Based

```kotlin
/**
 * Чанкование по токенам с overlap
 */
class TokenBasedChunking(
    private val tokenizer: Tokenizer
) : ChunkingStrategy {
    override val name = "token-based"
    
    override fun chunk(document: Document, config: ChunkingConfig): List<Chunk> {
        val tokens = tokenizer.tokenize(document.content)
        val chunks = mutableListOf<Chunk>()
        
        var startIndex = 0
        var chunkIndex = 0
        
        while (startIndex < tokens.size) {
            val endIndex = minOf(startIndex + config.maxChunkSize, tokens.size)
            val chunkTokens = tokens.subList(startIndex, endIndex)
            val chunkContent = chunkTokens.joinToString(" ")
            
            val startOffset = findOffset(document.content, tokens, startIndex)
            val endOffset = findOffset(document.content, tokens, endIndex - 1) + 
                tokens[endIndex - 1].length
            
            chunks.add(Chunk(
                id = IdGenerator.chunkId(document.id, chunkIndex),
                documentId = document.id,
                content = chunkContent,
                index = chunkIndex,
                startOffset = startOffset,
                endOffset = endOffset,
                metadata = ChunkMetadata(tokenCount = chunkTokens.size)
            ))
            
            chunkIndex++
            startIndex += config.maxChunkSize - config.overlap
        }
        
        return chunks
    }
}
```

### Sentence-Based

```kotlin
/**
 * Чанкование по предложениям
 */
class SentenceBasedChunking(
    private val tokenizer: Tokenizer
) : ChunkingStrategy {
    override val name = "sentence-based"
    
    private val sentenceDelimiters = Regex("[.!?]+\\s+")
    
    override fun chunk(document: Document, config: ChunkingConfig): List<Chunk> {
        val sentences = splitSentences(document.content)
        val chunks = mutableListOf<Chunk>()
        
        var currentChunk = StringBuilder()
        var currentTokenCount = 0
        var chunkStartOffset = 0
        var chunkIndex = 0
        var sentenceStartOffset = 0
        
        for (sentence in sentences) {
            val sentenceTokens = tokenizer.countTokens(sentence)
            
            if (currentTokenCount + sentenceTokens > config.maxChunkSize && 
                currentChunk.isNotEmpty()) {
                // Сохраняем текущий чанк
                chunks.add(createChunk(
                    document = document,
                    content = currentChunk.toString().trim(),
                    index = chunkIndex,
                    startOffset = chunkStartOffset,
                    tokenCount = currentTokenCount
                ))
                
                chunkIndex++
                
                // Overlap
                val (overlapContent, overlapTokens) = getOverlap(
                    chunks.last().content, 
                    config.overlap,
                    tokenizer
                )
                currentChunk = StringBuilder(overlapContent)
                currentTokenCount = overlapTokens
                chunkStartOffset = sentenceStartOffset - overlapContent.length
            }
            
            currentChunk.append(sentence)
            currentTokenCount += sentenceTokens
            sentenceStartOffset += sentence.length
        }
        
        // Последний чанк
        if (currentChunk.isNotEmpty()) {
            chunks.add(createChunk(
                document = document,
                content = currentChunk.toString().trim(),
                index = chunkIndex,
                startOffset = chunkStartOffset,
                tokenCount = currentTokenCount
            ))
        }
        
        return chunks
    }
    
    private fun splitSentences(text: String): List<String> {
        return text.split(sentenceDelimiters)
            .filter { it.isNotBlank() }
            .map { "$it. " }
    }
    
    private fun getOverlap(
        text: String, 
        targetTokens: Int, 
        tokenizer: Tokenizer
    ): Pair<String, Int> {
        val sentences = splitSentences(text).reversed()
        var overlap = StringBuilder()
        var tokenCount = 0
        
        for (sentence in sentences) {
            val sentenceTokens = tokenizer.countTokens(sentence)
            if (tokenCount + sentenceTokens > targetTokens) break
            overlap.insert(0, sentence)
            tokenCount += sentenceTokens
        }
        
        return overlap.toString() to tokenCount
    }
}
```

### Parent-Child

```kotlin
/**
 * Parent-Child чанкование
 * Parent — большие чанки для контекста LLM
 * Child — маленькие чанки для точного поиска
 */
class ParentChildChunking(
    private val childStrategy: ChunkingStrategy,
    private val tokenizer: Tokenizer
) : ChunkingStrategy {
    override val name = "parent-child"
    
    override fun chunk(document: Document, config: ChunkingConfig): List<Chunk> {
        // Parent чанки (большие)
        val parentConfig = config.copy(
            maxChunkSize = config.maxChunkSize * 3,
            overlap = 0
        )
        val parentChunks = createParentChunks(document, parentConfig)
        
        val allChunks = mutableListOf<Chunk>()
        
        parentChunks.forEachIndexed { parentIndex, parent ->
            allChunks.add(parent)
            
            // Child чанки для каждого parent
            val childDocument = document.copy(
                id = "${document.id}_parent_$parentIndex",
                content = parent.content
            )
            
            val children = childStrategy.chunk(childDocument, config)
                .map { child ->
                    child.copy(
                        id = IdGenerator.chunkId(document.id, allChunks.size),
                        documentId = document.id,
                        metadata = child.metadata.copy(
                            level = ChunkLevel.CHILD,
                            parentId = parent.id
                        )
                    )
                }
            
            allChunks.addAll(children)
        }
        
        return allChunks
    }
    
    private fun createParentChunks(
        document: Document, 
        config: ChunkingConfig
    ): List<Chunk> {
        val parentChunker = TokenBasedChunking(tokenizer)
        return parentChunker.chunk(document, config).map { chunk ->
            chunk.copy(metadata = chunk.metadata.copy(level = ChunkLevel.PARENT))
        }
    }
}
```

### Recursive (по структуре документа)

```kotlin
/**
 * Recursive чанкование по структуре документа (для MD/HTML)
 */
class RecursiveChunking(
    private val tokenizer: Tokenizer,
    private val fallbackStrategy: ChunkingStrategy
) : ChunkingStrategy {
    override val name = "recursive"
    
    override fun chunk(document: Document, config: ChunkingConfig): List<Chunk> {
        // Если есть структура — чанкуем по секциям
        // Иначе — fallback на другую стратегию
        return fallbackStrategy.chunk(document, config)
    }
}
```

---

## Chunker Facade

```kotlin
/**
 * Главный интерфейс для чанкования
 */
interface Chunker {
    fun chunk(document: Document): List<Chunk>
    fun chunk(content: String, documentId: String = IdGenerator.documentId()): List<Chunk>
}

class DefaultChunker(
    private val strategy: ChunkingStrategy,
    private val config: ChunkingConfig
) : Chunker {
    
    override fun chunk(document: Document): List<Chunk> {
        return strategy.chunk(document, config)
    }
    
    override fun chunk(content: String, documentId: String): List<Chunk> {
        val document = Document(id = documentId, content = content)
        return chunk(document)
    }
}
```

---

## Builder

```kotlin
class ChunkerBuilder {
    private var strategy: ChunkingStrategy? = null
    private var config: ChunkingConfig = ChunkingConfig()
    private var tokenizer: Tokenizer = SimpleTokenizer()
    
    fun tokenizer(tokenizer: Tokenizer) = apply { this.tokenizer = tokenizer }
    
    fun config(config: ChunkingConfig) = apply { this.config = config }
    
    fun config(block: ChunkingConfigBuilder.() -> Unit) = apply {
        this.config = ChunkingConfigBuilder().apply(block).build()
    }
    
    fun tokenBased() = apply { 
        strategy = TokenBasedChunking(tokenizer) 
    }
    
    fun sentenceBased() = apply { 
        strategy = SentenceBasedChunking(tokenizer) 
    }
    
    fun parentChild(childStrategy: ChunkingStrategy? = null) = apply {
        val child = childStrategy ?: SentenceBasedChunking(tokenizer)
        strategy = ParentChildChunking(child, tokenizer)
    }
    
    fun recursive(fallback: ChunkingStrategy? = null) = apply {
        val fb = fallback ?: SentenceBasedChunking(tokenizer)
        strategy = RecursiveChunking(tokenizer, fb)
    }
    
    fun strategy(strategy: ChunkingStrategy) = apply { 
        this.strategy = strategy 
    }
    
    fun build(): Chunker {
        val selectedStrategy = strategy ?: SentenceBasedChunking(tokenizer)
        return DefaultChunker(selectedStrategy, config)
    }
}

class ChunkingConfigBuilder {
    var maxChunkSize: Int = 512
    var minChunkSize: Int = 100
    var overlap: Int = 50
    var sizeUnit: SizeUnit = SizeUnit.TOKENS
    var preserveSentences: Boolean = true
    
    fun build() = ChunkingConfig(
        maxChunkSize = maxChunkSize,
        minChunkSize = minChunkSize,
        overlap = overlap,
        sizeUnit = sizeUnit,
        preserveSentences = preserveSentences
    )
}

// DSL entry point
fun chunker(block: ChunkerBuilder.() -> Unit): Chunker {
    return ChunkerBuilder().apply(block).build()
}
```

---

## Использование

```kotlin
// Простой chunker
val chunker = chunker {
    sentenceBased()
    config {
        maxChunkSize = 256
        overlap = 30
    }
}

val chunks = chunker.chunk("Long text content...")

// Parent-child chunking
val parentChildChunker = chunker {
    parentChild()
    config {
        maxChunkSize = 512
        overlap = 50
    }
}

val hierarchicalChunks = parentChildChunker.chunk(document)
```

---

## DI

```kotlin
val chunkingModule = module {
    single<Tokenizer> { SimpleTokenizer() }
    
    factory<ChunkingStrategy> { (type: String) ->
        val tokenizer: Tokenizer = get()
        when (type) {
            "token" -> TokenBasedChunking(tokenizer)
            "sentence" -> SentenceBasedChunking(tokenizer)
            "parent-child" -> ParentChildChunking(
                SentenceBasedChunking(tokenizer), 
                tokenizer
            )
            else -> SentenceBasedChunking(tokenizer)
        }
    }
    
    factory { (config: ChunkingConfig) ->
        DefaultChunker(get { parametersOf("sentence") }, config)
    }
}
```
