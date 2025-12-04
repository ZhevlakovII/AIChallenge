# Product Assistant Feature

Product Assistant - модуль для ответов на вопросы пользователей о продукте, используя RAG (FAQ) и MCP (тикеты поддержки).

## Структура модуля

### API Module (`features/productassistant/api`)
Минимальный модуль для навигационных контрактов.

### IMPL Module (`features/productassistant/impl`)
Полная реализация функциональности Product Assistant.

## Реализованные компоненты

### 1. FAQ (docs/FAQ.md)
Создан FAQ-файл с 8 пунктами на русском языке, включающий:
- Как работает чат с LLM
- Настройки LLM (API_KEY, baseUrl, model, topK)
- Где хранятся настройки
- Типичные проблемы:
  - Модель не отвечает
  - Неправильный API-ключ
  - Неверный URL
  - Модель не найдена
  - Слишком большое значение topK

### 2. Support Tickets (docs/support-tickets.json)
Создан JSON-файл с 8 тикетами поддержки на русском языке:
- Каждый тикет содержит: id, userId, title, description, status, createdAt, tags
- Различные типы проблем: авторизация, сеть, настройки, производительность, UI

### 3. MCP Server (instances/servers/mcp/support)
Реализован MCP-сервер для управления тикетами:
- **Порт**: 9001
- **WebSocket**: ws://127.0.0.1:9001/mcp
- **Инструменты**:
  - `support.list_tickets` - список всех тикетов (с фильтрами по status и tag)
  - `support.get_ticket` - получение конкретного тикета по ID

## Domain Layer

### Models
- **SupportTicket** - тикет поддержки
  - Fields: id, userId, title, description, status, createdAt, tags
  - Enum: `TicketStatus` (OPEN, IN_PROGRESS, RESOLVED)

- **DocumentationItem** - элемент FAQ
  - Fields: question, answer, category, keywords, relevanceScore
  - Enum: `DocumentationCategory` (CHAT_FUNCTIONALITY, LLM_SETTINGS, COMMON_ISSUES, etc.)

- **AssistantQuery** - запрос пользователя
  - Fields: text, mode, ticketId

- **AssistantResponse** - ответ ассистента
  - Fields: answer, mode, relatedTickets, relatedDocumentation, confidence, sources

- **AssistantMode** - режим работы ассистента:
  - **FAQ_ONLY** (Режим A) - только вопросы о продукте (FAQ + RAG)
  - **TICKET_ANALYSIS** (Режим B) - анализ тикетов (MCP)
  - **FULL** (Режим C) - полный режим (FAQ + MCP)

### Repository
**ProductAssistantRepository** - интерфейс репозитория с методами:
- `searchFaq(query, maxResults)` - поиск в FAQ
- `getAllTickets(statusFilter, tagFilter)` - получение всех тикетов
- `getTicketById(ticketId)` - получение тикета по ID
- `searchTickets(query, maxResults)` - поиск релевантных тикетов
- `generateAnswer(query, faqContext, ticketContext)` - генерация ответа через LLM

### Use Cases
1. **SearchFaqUseCase** / **SearchFaqUseCaseImpl**
   - Поиск релевантной информации в FAQ

2. **GetTicketUseCase** / **GetTicketUseCaseImpl**
   - Получение конкретного тикета по ID

3. **ListTicketsUseCase** / **ListTicketsUseCaseImpl**
   - Получение списка всех тикетов с фильтрацией

4. **GenerateAnswerUseCase** / **GenerateAnswerUseCaseImpl**
   - Генерация ответа с использованием LLM, RAG и MCP
   - Поддерживает 3 режима работы (FAQ_ONLY, TICKET_ANALYSIS, FULL)

## Data Layer

### Data Sources

1. **FaqDataSource** / **FaqDataSourceImpl**
   - Поиск по hardcoded FAQ с использованием scoring алгоритма
   - Учитывает вопросы, ответы и ключевые слова
   - Вычисляет relevance score для каждого результата

2. **RagSearchDataSource** / **RagSearchDataSourceImpl**
   - Полноценный RAG поиск по документации
   - Использует `RagSearchPipeline`, `RagEmbedder`, `RagRetriever`
   - Загружает индекс через `RagIndexRepository`
   - Проверяет настройки через `RagSettingsRepository`
   - Возвращает релевантные chunks как `DocumentationItem`

3. **TicketMcpDataSource** / **TicketMcpDataSourceImpl**
   - Работа с MCP сервером тикетов (ws://127.0.0.1:9001/mcp)
   - Инструменты: `support.list_tickets`, `support.get_ticket`

4. **LlmAnswerDataSource** / **LlmAnswerDataSourceImpl**
   - Генерация ответов через LLM
   - Использует `LLMClientRepository.sendMessagesWithSummary()`
   - Правильный способ обращения к LLM в проекте

### Repository Implementation
**ProductAssistantRepositoryImpl** - реализация репозитория:
- Объединяет FAQ (hardcoded), RAG, MCP и LLM data sources
- **searchFaq()** - комбинирует результаты из hardcoded FAQ и RAG поиска
- Вычисляет релевантность тикетов по запросу
- Генерирует промпты для LLM в зависимости от режима
- Вычисляет confidence score на основе контекста
- Использует `LLMClientRepository` для обращения к LLM

## Presentation Layer

### MVI Components

**State** (`ProductAssistantState`):
- query, selectedMode, isLoading, response, error, isInputEnabled

**Intent** (`ProductAssistantIntent`):
- QueryChanged, ModeChanged, AskQuestion, ViewTicket, ClearResponse, Retry

**Effect** (`ProductAssistantEffect`):
- ShowMessage, NavigateToTicket, ScrollToResponse

**Result** (`ProductAssistantResult`):
- QueryUpdated, ModeUpdated, LoadingStarted, AnswerReceived, ErrorOccurred, ResponseCleared

### UI Models
- **AssistantResponseUi** - UI модель ответа
- **TicketUi** - UI модель тикета
- **FaqItemUi** - UI модель FAQ
- **SourceUi** - UI модель источника информации

### Components

**ProductAssistantUiMapper** - маппер domain → UI моделей:
- Конвертация моделей для отображения
- Форматирование дат
- Маппинг цветов для статусов тикетов

**ProductAssistantExecutor** - обработка бизнес-логики:
- Обработка интентов пользователя
- Вызов use cases
- Генерация результатов и эффектов

**ProductAssistantViewModel** - MVI ViewModel:
- Управление состоянием
- Редьюсер для обновления state
- Обработка интентов через executor

**ProductAssistantScreen** - Compose UI:
- Селектор режима (3 режима)
- Поле ввода запроса
- Индикатор загрузки
- Отображение ошибок
- Отображение ответа с:
  - Основным текстом ответа
  - Связанными тикетами (кликабельные карточки)
  - Релевантной документацией (FAQ карточки)
  - Источниками информации
  - Confidence score с прогресс-баром

## Dependency Injection

**ProductAssistantModule** - Koin модуль:
- Data sources (FAQ, TicketMcp, LlmAnswer)
- Repository implementation
- Use cases
- UI Mapper
- Executor
- ViewModel

## Как работают режимы

### Режим A: FAQ Only
- Поиск только по FAQ
- Контекст: релевантные FAQ items
- Prompt: инструкции для работы с документацией

### Режим B: Ticket Analysis
- Поиск/получение тикетов
- Контекст: релевантные тикеты
- Prompt: инструкции для анализа проблем

### Режим C: Full Mode
- Поиск по FAQ + тикетам
- Контекст: FAQ items + tickets
- Prompt: комплексный анализ с использованием обоих источников

## Пример использования

```kotlin
// Inject ViewModel
val viewModel: ProductAssistantViewModel by koinViewModel()

// В Compose Screen
ProductAssistantScreen(viewModel = viewModel)

// Пользователь:
// 1. Выбирает режим (FAQ Only / Ticket Analysis / Full)
// 2. Вводит вопрос, например: "Почему не работает авторизация?"
// 3. Нажимает "Спросить"

// Система:
// 1. Ищет релевантную информацию в FAQ (если режим A или C)
// 2. Ищет релевантные тикеты через MCP (если режим B или C)
// 3. Генерирует промпт с контекстом
// 4. Отправляет запрос в LLM
// 5. Отображает ответ с источниками и связанными данными
```

## Зависимости

- `core:ui:mvi` - MVI framework
- `core:foundation` - Foundation utilities
- `shared:sharedold` - Shared components:
  - `McpRepository` - MCP client
  - `LLMClientRepository` - LLM client
  - `RagSettingsRepository` - RAG settings
  - `RagIndexRepository` - RAG index management
  - `RagEmbedder` - Embeddings generation
  - `RagRetriever` - Document retrieval
  - `RagSearchPipeline` - Full RAG pipeline
- `koin-core` - Dependency Injection
- `kotlinx-coroutines-core` - Coroutines
- `kotlinx-serialization-json` - JSON Serialization
- `kotlinx-datetime` - DateTime handling
- Compose Multiplatform - UI

## Package Structure

```
ru.izhxx.aichallenge.features.productassistant.impl
├── domain
│   ├── model
│   │   ├── SupportTicket.kt
│   │   ├── DocumentationItem.kt
│   │   ├── AssistantQuery.kt
│   │   └── AssistantResponse.kt
│   ├── repository
│   │   └── ProductAssistantRepository.kt
│   └── usecase
│       ├── SearchFaqUseCase.kt
│       ├── GetTicketUseCase.kt
│       ├── ListTicketsUseCase.kt
│       └── GenerateAnswerUseCase.kt
├── data
│   ├── datasource
│   │   ├── FaqDataSource.kt (hardcoded FAQ)
│   │   ├── FaqDataSourceImpl.kt
│   │   ├── RagSearchDataSource.kt (RAG search)
│   │   ├── RagSearchDataSourceImpl.kt
│   │   ├── TicketMcpDataSource.kt (MCP tickets)
│   │   ├── TicketMcpDataSourceImpl.kt
│   │   ├── LlmAnswerDataSource.kt (LLM client)
│   │   └── LlmAnswerDataSourceImpl.kt
│   └── repository
│       └── ProductAssistantRepositoryImpl.kt
├── presentation
│   ├── model
│   │   ├── ProductAssistantState.kt
│   │   ├── ProductAssistantIntent.kt
│   │   ├── ProductAssistantEffect.kt
│   │   └── ProductAssistantResult.kt
│   ├── mapper
│   │   └── ProductAssistantUiMapper.kt
│   ├── ProductAssistantExecutor.kt
│   ├── ProductAssistantViewModel.kt
│   └── ProductAssistantScreen.kt
└── di
    └── ProductAssistantModule.kt
```

## Next Steps

1. Добавить модуль в основное приложение:
   - Зарегистрировать `productAssistantModule` в Koin
   - Добавить навигационный роут
   - Добавить экран в навигационный граф

2. Запустить MCP сервер:
   ```bash
   ./gradlew :instances:servers:mcp:support:run
   ```

3. Протестировать фичу:
   - Режим FAQ: вопросы о продукте
   - Режим Ticket Analysis: анализ конкретных проблем
   - Режим Full: комбинированный анализ

4. Улучшения (опционально):
   - Интеграция настоящего RAG вместо hardcoded FAQ
   - Кэширование ответов
   - Сохранение истории запросов
   - Feedback система для улучшения ответов
