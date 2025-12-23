# Архитектура RAG-модуля

## Обзор

RAG (Retrieval-Augmented Generation) модуль — набор независимых компонентов для построения RAG-pipeline. Каждый компонент можно использовать отдельно или комбинировать в готовые решения.

**Принципы:**
- Независимость компонентов — можно использовать только chunking или только embedding
- Runtime конфигурация — builder pattern для сборки pipeline
- Расширяемость — легко добавить новый parser, chunker, embedder
- Локальное хранение — Room + FTS для hybrid search

---

## Структура модулей

```
tool/
└── rag/
    ├── core/                      # Базовые абстракции и модели
    ├── parser/                    # Парсинг документов (MD, HTML, PDF, DOCX)
    ├── chunking/                  # Стратегии чанкования
    ├── embedding/                 # Генерация эмбеддингов
    ├── storage/                   # Векторное хранилище + FTS
    ├── retrieval/                 # Поиск (semantic, hybrid, reranking)
    └── pipeline/                  # Высокоуровневый API, объединяющий всё
```

---

## Граф зависимостей

```
:tool:rag:pipeline
    ├── :tool:rag:retrieval
    │       ├── :tool:rag:storage
    │       │       └── :tool:rag:core
    │       └── :tool:rag:embedding
    │               └── :tool:rag:core
    ├── :tool:rag:chunking
    │       └── :tool:rag:core
    └── :tool:rag:parser
            └── :tool:rag:core

:tool:rag:core
    ├── :core:common
    └── :core:model
```

**Правило:** Пользователь может подключить любой набор модулей:
- Только `:tool:rag:chunking` — для разбиения текста
- Только `:tool:rag:embedding` — для генерации эмбеддингов
- `:tool:rag:pipeline` — полный RAG с автоматическим подтягиванием зависимостей

---

## Документация модулей

| Модуль                | Документация                   | Описание                                                  |
|-----------------------|--------------------------------|-----------------------------------------------------------|
| `:tool:rag:core`      | [core.md](./core.md)           | Базовые модели: Document, Chunk, Collection, SearchResult |
| `:tool:rag:parser`    | [parser.md](./parser.md)       | Парсеры документов: Markdown, HTML, PDF, DOCX             |
| `:tool:rag:chunking`  | [chunking.md](./chunking.md)   | Стратегии чанкования: token, sentence, parent-child       |
| `:tool:rag:embedding` | [embedding.md](./embedding.md) | Генерация эмбеддингов: Ollama, OpenAI + кэширование       |
| `:tool:rag:storage`   | [storage.md](./storage.md)     | Векторное хранилище (Room) + FTS + Document tracking      |
| `:tool:rag:retrieval` | [retrieval.md](./retrieval.md) | Поиск: semantic, hybrid (RRF), reranking                  |
| `:tool:rag:pipeline`  | [pipeline.md](./pipeline.md)   | Высокоуровневый API + DSL builder                         |

---

## Сценарии использования

### 1. Только чанкование

```kotlin
dependencies {
    implementation(":tool:rag:chunking")
}

val chunker = chunker {
    sentenceBased()
    config { maxChunkSize = 256 }
}

val chunks = chunker.chunk("Long text...")
```

### 2. Только эмбеддинги

```kotlin
dependencies {
    implementation(":tool:rag:embedding")
}

val embedder = embedder {
    httpClient(client)
    ollama("http://localhost:11434", "nomic-embed-text", 768)
    cached()
}

val embedding = embedder.embed("Some text")
```

### 3. Полный RAG pipeline

```kotlin
dependencies {
    implementation(":tool:rag:pipeline")
}

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
    }
    
    chunking {
        parentChild()
    }
}

// Использование
val collection = pipeline.createCollection("my-docs")
pipeline.indexFile(collection.id, "/path/to/document.md")

val result = pipeline.query(collection.id, "What is the main topic?")
println(result.augmentedPrompt)
```

---

## DI конфигурация

```kotlin
// Подключение всех RAG-модулей
val ragModules = listOf(
    parserModule,
    chunkingModule,
    embeddingModule,
    storageModule,
    retrievalModule,
    pipelineModule
)

// В Application
startKoin {
    modules(ragModules)
}
```
