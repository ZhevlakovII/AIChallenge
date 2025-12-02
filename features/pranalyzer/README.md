# PR Analyzer Feature

Pull Request Analyzer - модуль для анализа GitHub Pull Requests с использованием LLM и RAG.

## Структура модуля

### API Module (`features/pranalyzer/api`)
Минимальный модуль для будущих навигационных контрактов.

### IMPL Module (`features/pranalyzer/impl`)
Полная реализация функциональности PR Analyzer.

## Domain Layer

### Models

#### Core Models
- **PullRequest** - основная информация о Pull Request
  - Поля: number, title, description, author, branches, state, timestamps, statistics
  - Enum: `PrState` (OPEN, CLOSED, MERGED, DRAFT)

- **PrFile** - информация о файле, измененном в PR
  - Поля: filename, status, additions, deletions, patch, URLs
  - Enum: `FileStatus` (ADDED, MODIFIED, REMOVED, RENAMED, etc.)

- **PrDiff** - полная информация о diff PR
  - Поля: prNumber, files, totalAdditions, totalDeletions, totalChanges
  - Связанные классы: `DiffHunk`, `FileDiff`

#### Analysis Models
- **CodeIssue** - найденная проблема в коде
  - Поля: severity, category, title, description, file, lineNumber, codeSnippet, suggestion
  - Enums: `IssueSeverity` (CRITICAL, HIGH, MEDIUM, LOW, INFO)
  - Enums: `IssueCategory` (BUG, SECURITY, PERFORMANCE, CODE_STYLE, etc.)

- **Recommendation** - рекомендация по улучшению
  - Поля: priority, category, title, description, rationale, relatedFiles, actionableSteps
  - Enums: `RecommendationPriority` (MUST_HAVE, SHOULD_HAVE, NICE_TO_HAVE, OPTIONAL)
  - Enums: `RecommendationCategory` (REFACTORING, TESTING, DOCUMENTATION, etc.)

- **DocumentationReference** - ссылка на документацию
  - Поля: title, url, filePath, relevanceScore, summary, keywords, section
  - Связанный класс: `DocumentationSearchResult`

- **LlmAnalysis** - результат LLM анализа
  - Поля: prNumber, summary, strengths, weaknesses, issues, recommendations
  - Поля: scores (overall, readability, maintainability, security)
  - Поля: testCoverageAssessment, architecturalNotes, relevantDocumentation
  - Связанный класс: `QualityScores`

- **AnalysisReport** - финальный отчет анализа
  - Поля: pullRequest, diff, llmAnalysis, generatedAt, analysisVersion
  - Связанный класс: `AnalysisReportSummary`
  - Extension function: `toSummary()`

### Repository

**PrAnalyzerRepository** - интерфейс репозитория
- `fetchPrInfo(owner, repo, prNumber)` - получить информацию о PR
- `fetchPrDiff(owner, repo, prNumber)` - получить diff PR
- `fetchFileContent(owner, repo, filePath, ref)` - получить содержимое файла
- `searchRelevantDocumentation(query, maxResults)` - поиск документации
- `analyzePrWithLlm(pullRequest, diff, documentation)` - анализ с помощью LLM

### Use Cases

1. **FetchPrInfoUseCase** / **FetchPrInfoUseCaseImpl**
   - Получение информации о Pull Request
   - Валидация параметров (owner, repo, prNumber)

2. **FetchPrDiffUseCase** / **FetchPrDiffUseCaseImpl**
   - Получение diff Pull Request
   - Валидация параметров

3. **SearchRelevantDocsUseCase** / **SearchRelevantDocsUseCaseImpl**
   - Поиск релевантной документации
   - Построение поискового запроса из PR title, description, changed files
   - Извлечение ключевых слов

4. **AnalyzePrWithLlmUseCase** / **AnalyzePrWithLlmUseCaseImpl**
   - Анализ PR с помощью LLM
   - Валидация соответствия PR number между объектами

5. **GenerateReportUseCase** / **GenerateReportUseCaseImpl**
   - Генерация финального отчета
   - Валидация целостности данных
   - Установка timestamp и version

## Dependencies

- `core:ui:mvi` - MVI framework
- `core:foundation` - Foundation utilities
- `koin-core` - Dependency Injection
- `kotlinx-coroutines-core` - Coroutines
- `kotlinx-serialization-json` - JSON Serialization
- `kotlinx-datetime` - DateTime handling

## Package Structure

```
ru.izhxx.aichallenge.features.pranalyzer.impl
├── domain
│   ├── model
│   │   ├── AnalysisReport.kt
│   │   ├── CodeIssue.kt
│   │   ├── DocumentationReference.kt
│   │   ├── LlmAnalysis.kt
│   │   ├── PrDiff.kt
│   │   ├── PrFile.kt
│   │   ├── PullRequest.kt
│   │   └── Recommendation.kt
│   ├── repository
│   │   └── PrAnalyzerRepository.kt
│   └── usecase
│       ├── AnalyzePrWithLlmUseCase.kt
│       ├── AnalyzePrWithLlmUseCaseImpl.kt
│       ├── FetchPrDiffUseCase.kt
│       ├── FetchPrDiffUseCaseImpl.kt
│       ├── FetchPrInfoUseCase.kt
│       ├── FetchPrInfoUseCaseImpl.kt
│       ├── GenerateReportUseCase.kt
│       ├── GenerateReportUseCaseImpl.kt
│       ├── SearchRelevantDocsUseCase.kt
│       └── SearchRelevantDocsUseCaseImpl.kt
├── data (TODO)
│   └── repository (TODO)
├── di (TODO)
└── presentation (TODO)
```

## Next Steps

1. Реализовать Data Layer:
   - GitHub API client
   - LLM client integration
   - Documentation search client
   - PrAnalyzerRepositoryImpl

2. Реализовать DI Module:
   - Koin module с провайдингом всех dependencies

3. Реализовать Presentation Layer:
   - MVI State, Event, Effect models
   - ViewModel
   - Compose UI screens

## Usage Example (Conceptual)

```kotlin
// Inject use cases via Koin
val fetchPrInfo: FetchPrInfoUseCase by inject()
val fetchPrDiff: FetchPrDiffUseCase by inject()
val searchDocs: SearchRelevantDocsUseCase by inject()
val analyzePr: AnalyzePrWithLlmUseCase by inject()
val generateReport: GenerateReportUseCase by inject()

// Execute analysis pipeline
val prInfo = fetchPrInfo("owner", "repo", 123).getOrThrow()
val prDiff = fetchPrDiff("owner", "repo", 123).getOrThrow()
val docs = searchDocs(prInfo, prDiff).getOrThrow()
val analysis = analyzePr(prInfo, prDiff, docs).getOrThrow()
val report = generateReport(prInfo, prDiff, analysis).getOrThrow()
```
