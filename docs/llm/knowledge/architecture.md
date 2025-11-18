# Архитектура (сводка для LLM)

Краткое резюме архитектуры AIChallenge для генерации кода. Подробно см.: docs/human/Architecture.md, docs/human/FeatureDevelopmentGuide.md, docs/llm/file_paths_policy.md, docs/llm/code_generation_rules.md. Список ключевых файлов: docs/inventory/project_inventory.json.

Содержание:
- Слои и зависимости
- MVI-поток (UI)
- KMP-организация
- DI (Koin)
- Данные (API/БД/DataStore)
- Ошибки и логирование
- Быстрые ссылки

## Слои и зависимости

- Presentation (UI/MVI) — composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/presentation
  - Содержит Compose UI, ViewModel, модели `State`, `Event` (+опц. `Effect` через SharedFlow).
  - Зависит только от Domain-контрактов/UseCase-ов. DTO запрещены.

- Domain — shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain
  - Чистые модели, интерфейсы репозиториев и UseCase-ы.
  - Не зависит от платформ/фреймворков. Определяет контракты (DIP).

- Data — shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data
  - Реализации репозиториев, источники (Ktor/DAO/DataStore), DTO, мапперы DTO↔Domain.
  - Нормализует ошибки, инкапсулирует детали инфраструктуры.

Инварианты:
- DTO не пересекают границу Presentation/Domain.
- Presentation → Domain (контракты), Domain → ничего, Data → реализует Domain.

## MVI-поток (UI)

- Event (sealed class) → ViewModel.processEvent(event) → UseCase → Repository → (Domain models) → ViewModel.set State → UI.
- ViewModel:
  - `private val _state = MutableStateFlow(State(...))`
  - `val state: StateFlow<State> = _state.asStateFlow()`
  - Обновления только через `_state.update { it.copy(...) }`
- UI:
  - Подписка: `collectAsStateWithLifecycle()`
  - Только диспатчит события и рендерит `State`.

Шаблоны: см. docs/human/FeatureDevelopmentGuide.md и docs/llm/templates/FeatureViewModel.kt.md.

## KMP-организация

- Максимум логики — в `commonMain`.
- Платформенная специфика — в `androidMain`/`jvmMain` за интерфейсами (DI).
- Основные модули:
  - composeApp — UI/навигация/фичи.
  - shared — Domain/Data/DI/утилиты.
  - server — Ktor-сервер (опц.).

Пути и примеры: docs/llm/file_paths_policy.md, docs/human/ProjectStructure.md.

## DI (Koin)

- Общие DI-модули: `shared/.../di/*.kt` (SharedModule, ParsersModule, MetricsModule, McpModule, CompressionModule).
- Фичевые DI: `composeApp/.../features/<feature>/di/<Feature>Module.kt`
- Регистрация по интерфейсам Domain; никаких `new` внутри ViewModel.

## Данные (API/БД/DataStore)

- API: Ktor-клиент + Kotlinx.serialization, DTO лежат в `shared/.../data/model/*DTO.kt`.
- БД/DAO/Entity: `shared/.../data/database/{dao,entity}`; фабрики: `DatabaseFactory`, `AppDatabase`.
- DataStore (секреты/конфиги): контракт в `shared/.../di/DataStoreProvider.kt`, реализации в `androidMain`/`jvmMain`.
- Мапперы: extension-функции рядом с реализациями (Data), DTO/Entity ↔ Domain.

## Ошибки и логирование

- Все внешние вызовы оборачивать в `safeApiCall { ... }`:
  - Низкоуровневые: `RequestError`, `ApiError` (Data).
  - Доменная форма: `DomainException` (Domain) — единственная для Presentation.
- Логирование через общий `Logger` (`shared/common/Logger.kt`), маскирование секретов (Authorization/api-key) обязательно.
- Метрики UI: экран `ChatMetricsScreen`.

Подробнее: docs/human/ErrorHandling.md, docs/human/API-Networking.md, docs/human/Logging-Metrics.md.

## Быстрые ссылки

- Старт UI/Навигация: composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/App.kt
- DI общие: shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/
- API OpenAI: shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/api/OpenAIApi*.kt
- Safe API: shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/SafeApiCall.kt
- Пример фичи: composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/chat/**
- Инвентарь: docs/inventory/project_inventory.json
