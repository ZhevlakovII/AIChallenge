# Изменения в Product Assistant

## Исправления после review

### 1. Исправлен LlmAnswerDataSource

**Было:**
```kotlin
val flow = callLlmStreamingUseCase(messages) // Ошибка - переменная не существует
flow.collect { chunk -> response.append(chunk) }
```

**Стало:**
```kotlin
val llmResponse = llmClientRepository.sendMessagesWithSummary(messages, null)
    .getOrThrow()
llmResponse.text.ifBlank { ... }
```

**Причина:** Правильный способ обращения к LLM в проекте - через `LLMClientRepository.sendMessagesWithSummary()`.

### 2. Добавлен полноценный RAG поиск

Создан новый data source:
- `RagSearchDataSource` / `RagSearchDataSourceImpl`

**Использует:**
- `RagSearchPipeline` - основной pipeline для RAG
- `RagEmbedder` - генерация embeddings
- `RagRetriever` - поиск релевантных chunks
- `RagIndexRepository` - управление индексом
- `RagSettingsRepository` - настройки RAG

**Алгоритм:**
1. Проверяет включен ли RAG в настройках
2. Загружает индекс (если еще не загружен)
3. Создает RAG pipeline
4. Получает релевантные chunks
5. Конвертирует chunks в `DocumentationItem`

### 3. Обновлен ProductAssistantRepositoryImpl

**Было:**
```kotlin
override suspend fun searchFaq(query: String, maxResults: Int): Result<List<DocumentationItem>> {
    return faqDataSource.searchFaq(query, maxResults)
}
```

**Стало:**
```kotlin
override suspend fun searchFaq(query: String, maxResults: Int): Result<List<DocumentationItem>> {
    return runCatching {
        // Combine results from both FAQ and RAG
        val faqResults = faqDataSource.searchFaq(query, maxResults).getOrDefault(emptyList())
        val ragResults = ragSearchDataSource.searchDocumentation(query, maxResults).getOrDefault(emptyList())

        // Combine and sort by relevance score
        val combined = (faqResults + ragResults)
            .sortedByDescending { it.relevanceScore }
            .take(maxResults)

        combined
    }
}
```

**Преимущества:**
- Комбинирует результаты из hardcoded FAQ и RAG
- Сортирует по relevance score
- Fallback при ошибках - возвращает хотя бы один источник

### 4. Обновлен DI модуль

Добавлен:
```kotlin
singleOf(::RagSearchDataSourceImpl) bind RagSearchDataSource::class
```

**Зависимости RAG** (инжектятся автоматически через Koin):
- `RagSettingsRepository`
- `RagIndexRepository`
- `RagEmbedder`
- `RagRetriever`

Эти зависимости должны быть предоставлены shared модулем.

## Архитектура поиска

### Двухуровневый поиск по FAQ:

1. **Hardcoded FAQ** (`FaqDataSourceImpl`):
   - 8 предопределенных вопросов-ответов
   - Быстрый scoring по ключевым словам
   - Всегда доступен

2. **RAG Search** (`RagSearchDataSourceImpl`):
   - Поиск по индексированной документации
   - Semantic search через embeddings
   - Требует настроенный RAG

### Комбинирование результатов:

```
User Query
    ↓
┌───────────────┐       ┌───────────────┐
│ Hardcoded FAQ │       │  RAG Search   │
│   (scoring)   │       │  (semantic)   │
└───────┬───────┘       └───────┬───────┘
        │                       │
        └───────┬───────────────┘
                ↓
        Combine & Sort by
        relevance score
                ↓
        Top N results
```

## Проверка работоспособности

### 1. LLM Client
```kotlin
// Теперь работает правильно
val response = llmClientRepository.sendMessagesWithSummary(messages, null)
println(response.text)
```

### 2. RAG Search
```kotlin
// Если RAG включен - вернет результаты из индекса
val ragResults = ragSearchDataSource.searchDocumentation("API ключ", 5)

// Если RAG выключен или индекс не загружен - вернет пустой список
```

### 3. Combined Search
```kotlin
// Всегда вернет результаты хотя бы из одного источника
val results = repository.searchFaq("проблема с авторизацией", 5)
// results может содержать: hardcoded FAQ + RAG chunks
```

## Конфигурация

Убедитесь что в настройках RAG:
- `ragSettings.enabled = true` (если нужен RAG поиск)
- `ragSettings.indexPath` указывает на валидный индекс
- Индекс загружен через `ragIndexRepository.loadIndex()`

Если RAG не настроен - Product Assistant будет работать только с hardcoded FAQ.
