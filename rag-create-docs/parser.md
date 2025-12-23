# :tool:rag:parser

Парсинг документов разных форматов в унифицированный текст.

**Зависимости:** `:tool:rag:core`, `:core:common`

---

## Абстракция

```kotlin
/**
 * Парсер документа определённого формата
 */
interface DocumentParser {
    val supportedTypes: Set<DocumentType>
    
    suspend fun parse(input: ParserInput): ParsedDocument
    
    fun canParse(input: ParserInput): Boolean {
        return input.type in supportedTypes
    }
}

sealed class ParserInput {
    abstract val type: DocumentType
    
    data class FilePath(
        val path: String,
        override val type: DocumentType
    ) : ParserInput()
    
    data class Content(
        val content: ByteArray,
        override val type: DocumentType,
        val fileName: String? = null
    ) : ParserInput()
}

data class ParsedDocument(
    val content: String,
    val metadata: DocumentMetadata,
    val structure: DocumentStructure? = null
)

/**
 * Структура документа (для продвинутого чанкования)
 */
data class DocumentStructure(
    val sections: List<Section>
)

data class Section(
    val title: String?,
    val level: Int,              // h1=1, h2=2, etc.
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
    val children: List<Section> = emptyList()
)
```

---

## Реестр парсеров

```kotlin
class ParserRegistry {
    private val parsers = mutableListOf<DocumentParser>()
    
    fun register(parser: DocumentParser) {
        parsers.add(parser)
    }
    
    fun getParser(type: DocumentType): DocumentParser? {
        return parsers.find { type in it.supportedTypes }
    }
    
    fun getParser(input: ParserInput): DocumentParser? {
        return parsers.find { it.canParse(input) }
    }
    
    suspend fun parse(input: ParserInput): ParsedDocument {
        val parser = getParser(input) 
            ?: throw UnsupportedDocumentTypeException(input.type)
        return parser.parse(input)
    }
}

class UnsupportedDocumentTypeException(
    val type: DocumentType
) : Exception("No parser registered for type: $type")
```

---

## Реализации

### Markdown парсер (MVP)

```kotlin
class MarkdownParser : DocumentParser {
    override val supportedTypes = setOf(DocumentType.MARKDOWN)
    
    override suspend fun parse(input: ParserInput): ParsedDocument {
        val rawContent = when (input) {
            is ParserInput.FilePath -> readFile(input.path)
            is ParserInput.Content -> input.content.decodeToString()
        }
        
        val structure = parseStructure(rawContent)
        val metadata = extractMetadata(rawContent)
        
        return ParsedDocument(
            content = rawContent,
            metadata = metadata.copy(type = DocumentType.MARKDOWN),
            structure = structure
        )
    }
    
    private fun parseStructure(content: String): DocumentStructure {
        val sections = mutableListOf<Section>()
        val headerRegex = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
        
        headerRegex.findAll(content).forEach { match ->
            val level = match.groupValues[1].length
            val title = match.groupValues[2]
            val startOffset = match.range.first
            
            sections.add(Section(
                title = title,
                level = level,
                content = "",
                startOffset = startOffset,
                endOffset = startOffset
            ))
        }
        
        return DocumentStructure(buildSectionTree(sections, content))
    }
    
    private fun extractMetadata(content: String): DocumentMetadata {
        // Извлечение YAML frontmatter если есть
        val frontmatterRegex = Regex("^---\\n([\\s\\S]*?)\\n---")
        val match = frontmatterRegex.find(content)
        
        return if (match != null) {
            parseFrontmatter(match.groupValues[1])
        } else {
            DocumentMetadata()
        }
    }
    
    private fun parseFrontmatter(yaml: String): DocumentMetadata {
        val properties = yaml.lines()
            .filter { it.contains(":") }
            .associate { line ->
                val (key, value) = line.split(":", limit = 2)
                key.trim() to value.trim()
            }
        
        return DocumentMetadata(
            title = properties["title"],
            author = properties["author"],
            tags = properties["tags"]
                ?.removeSurrounding("[", "]")
                ?.split(",")
                ?.map { it.trim() } 
                ?: emptyList()
        )
    }
}
```

### HTML парсер

```kotlin
class HtmlParser : DocumentParser {
    override val supportedTypes = setOf(DocumentType.HTML)
    
    override suspend fun parse(input: ParserInput): ParsedDocument {
        // Используем Ksoup или аналог для парсинга HTML
        TODO("Реализация HTML парсера")
    }
}
```

### PDF парсер (expect/actual)

```kotlin
// commonMain
expect class PdfParser() : DocumentParser

// jvmMain — Apache PDFBox
actual class PdfParser : DocumentParser {
    override val supportedTypes = setOf(DocumentType.PDF)
    
    override suspend fun parse(input: ParserInput): ParsedDocument {
        // PDFBox implementation
    }
}

// androidMain — PdfRenderer или iText
actual class PdfParser : DocumentParser {
    // Android implementation
}
```

### DOCX парсер (expect/actual)

```kotlin
// commonMain
expect class DocxParser() : DocumentParser

// jvmMain — Apache POI
// androidMain — Apache POI Android или docx4j-android
```

---

## DI

```kotlin
val parserModule = module {
    single { ParserRegistry() }
    single { MarkdownParser() }
    
    // Регистрация парсеров
    single {
        get<ParserRegistry>().apply {
            register(get<MarkdownParser>())
            // register(get<HtmlParser>())
            // register(get<PdfParser>())
            // register(get<DocxParser>())
        }
    }
}
```
