# Архитектура проекта

Документ описывает целевую архитектуру AIChallenge. Основан на принципах Чистой архитектуры, KMP (Kotlin Multiplatform), MVI (Model–View–Intent), DI (Koin), сетевом стеке Ktor и асинхронности Coroutines/StateFlow. Согласован с AIChallenge-StyleGuide.md, .clinerules/Project-rules.md и сопровождающими документами:
- CodingStandards.md
- FeatureDevelopmentGuide.md
- ErrorHandling.md
- API-Networking.md
- Data-Persistence.md
- Logging-Metrics.md
- TestingStrategy.md
- Security.md

См. также инвентаризацию кода: docs/inventory/project_inventory.json

Содержание:
- Цели и принципы
- Слоистая модель (Presentation / Domain / Data)
- Паттерн MVI и поток данных
- KMP-организация и модули
- Внедрение зависимостей (Koin)
- Навигация
- Работа с данными (API, БД, DataStore)
- Обработка ошибок
- Логирование и метрики
- Диаграмма потоков (текстовое описание)
- Решения и компромиссы
- Чек-лист соответствия

## Цели и принципы

- Чистая архитектура и инверсия зависимостей: верхние слои не знают деталей нижних, взаимодействие через контракты Domain.
- Однонаправленный поток данных в UI (MVI): Event → ViewModel → UseCase → Repository → State.
- KMP: максимальная доля общего кода в commonMain; платформенная специфика — в соответствующих source sets.
- Модульность по фичам: каждая фича имеет собственные пакеты `di`, `domain`, `presentation`, `data` в составе UI-модуля; общие реализации — в `shared`.
- Тестируемость: UseCase-ы и мапперы — чистые, UI-логика в ViewModel — детерминированная, StateFlow протестирован.
- Безопасность и наблюдаемость: единые политики для ошибок, логов и метрик.

## Слоистая модель

- Presentation (UI/MVI) — composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/presentation
  - Compose UI, ViewModel, модели `State`/`Event` (+ опционально `Effect` через SharedFlow).
  - Подписка на состояние `collectAsStateWithLifecycle()`; события отправляются в ViewModel.
  - Зависит только от Domain-контрактов и UseCase-ов.

- Domain — shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain
  - Чистые доменные модели, интерфейсы репозиториев, use case.
  - Не содержит платформенных или инфраструктурных зависимостей.
  - Определяет контракты (DIP), на которые опирается Presentation.

- Data — shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data
  - Реализации репозиториев, источники данных (API/DAO/DataStore), DTO-модели, мапперы.
  - Сетевая логика Ktor, сериализация Kotlinx.serialization.
  - Нормализация ошибок (safeApiCall), преобразование DTO ↔ Domain.

Инварианты:
- DTO не пересекают границу Presentation/Domain.
- Domain зависит только от себя; Presentation не зависит от Data.
- Ошибки в Presentation — только в доменной форме (DomainException).

## Паттерн MVI и поток данных

- Event: намерения пользователя/системные события (sealed class).
- ViewModel:
  - Хранит `MutableStateFlow<State>` и экспонирует `StateFlow<State>`.
  - Обрабатывает события в `processEvent(event)`, вызывает UseCase-ы.
  - Обновляет состояние исключительно через `_state.update { ... }`.
- State: неизменяемая data class, описывает всё, что нужно для рендера экрана.
- Effect (опц.): одноразовые события (навигация, snackbar) — через `SharedFlow`.

Универсальный цикл:
1) UI диспатчит `Event` → 2) VM вызывает UseCase → 3) Repository обращается к источникам → 4) Результат маппится в доменные модели → 5) VM обновляет `State` → 6) UI рендерит.

См. FeatureDevelopmentGuide.md для шаблонов.

## KMP-организация и модули

- composeApp — UI и фичи (Compose MPP), навигация, ViewModel:
  - commonMain: общие UI-компоненты и ViewModel.
  - androidMain/jvmMain: платформенные адаптеры ресурсов/инициализации.
- shared — общий доменно-данный слой:
  - commonMain: доменные контракты, реализации репозиториев, API, DAO, мапперы, DI-модули общего назначения.
  - androidMain/jvmMain: платформенная реализация `DataStoreProvider`, логгера и др.
- server — Ktor-сервер (при необходимости), отдельный процесс/слой интеграции.

Пути и раскладка описаны в docs/llm/file_paths_policy.md и документе ProjectStructure.md (см. ниже).

## Внедрение зависимостей (Koin)

- Общие DI-модули: shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/*
  - SharedModule, ParsersModule, MetricsModule, McpModule, CompressionModule и др.
- Модули фич: composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/di/<Feature>Module.kt
  - Регистрация UseCase-ов, репозиториев (через контракты), ViewModel.

Правила:
- Регистрация по интерфейсам Domain.
- Платформенная специфика (например, DataStore) — через провайдеры в platform source sets.
- Никаких “new” внутри ViewModel: зависимости предоставляются DI.

## Навигация

- Точка входа: composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/App.kt
- Маршруты в виде sealed class Screen.
- NavHost определяет экраны; параметры прокидываются через аргументы маршрута.
- Навигация инициируется из UI через колбэки; ViewModel не содержит прямых вызовов навигации.

## Работа с данными

- API (Ktor):
  - Конфигурация клиента через DI (см. API-Networking.md).
  - DTO расположены в shared/.../data/model, сериализация через Kotlinx.serialization.
  - Маскирование секретов и логирование с корреляционными ID.
- БД/DAO:
  - DAO/Entity в shared/.../data/database/{dao,entity}; фабрики/инициализация через DatabaseFactory.
  - Транзакции и миграции (если используются) инкапсулированы в Data.
- DataStore/конфиги:
  - Провайдер `DataStoreProvider` в shared/.../di, платформенные реализации в androidMain/jvmMain.
  - Ключи/конфиги не попадают в UI, используются в Data/DI.

## Обработка ошибок

- Низкоуровневые ошибки: RequestError/ApiError (Data).
- Доменная ошибка: DomainException (Domain); единственная форма для Presentation.
- Все внешние вызовы оборачивать в `safeApiCall { ... }`.
- UI отображает user-friendly сообщения, без технических деталей (см. ErrorHandling.md).

## Логирование и метрики

- Общий Logger (shared/common/Logger.kt), уровень и формат кросс-платформенные.
- Маскирование секретов (Authorization/api-key) обязательно.
- Бизнес-метрики и технические метрики; экран метрик: ChatMetricsScreen (см. Logging-Metrics.md).

## Диаграмма потоков (текстовое описание)

- Сценарий «Отправка сообщения в чат»:
  1) Пользователь вводит текст и нажимает Send → UI диспатчит `ChatEvent.Send(text)`.
  2) ChatViewModel.processEvent вызывает UseCase `SendMessageUseCase`.
  3) UseCase обращается к `LLMClientRepository` (Domain контракт).
  4) В Data-реализации репозитория вызов клиента Ktor (OpenAIApiImpl) обёрнут `safeApiCall`.
  5) Ответ DTO маппится в доменную модель `LLMResponse`, ошибки нормализуются в `DomainException`.
  6) ViewModel обновляет `ChatUiState` (loading=false, messages+=response), или `error=DomainException`.
  7) UI рендерит новое состояние, при ошибке показывает Snackbar/баннер и предлагает Retry.

## Решения и компромиссы

- DTO-изоляция: усложняет маппинг, но делает слои независимыми и тестируемыми.
- KMP: переносит максимум логики в commonMain, платформенные ограничения решаются через DI-интерфейсы.
- MVI: упрощает reasoning и тестирование, но требует дисциплины состояния и событий.
- Koin: ускоряет сборку зависимостей, следует избегать глобального синглтон-стиля в тестах — подменять модули.

## Чек-лист соответствия

- [ ] Presentation зависит только от Domain (никаких DTO/реализаций Data).
- [ ] Domain не зависит от фреймворков и платформенных API.
- [ ] Data реализует контракты Domain; API/DAO/DTO/мапперы расположены в правильных директориях.
- [ ] MVI: State/Event оформлены, ViewModel обновляет состояние через StateFlow.
- [ ] Все внешние вызовы обёрнуты в `safeApiCall`, ошибки нормализуются к DomainException.
- [ ] DI: зависимости зарегистрированы в модулях фич и общих модулях.
- [ ] Навигация определена централизованно, параметры маршрутов корректны.
- [ ] Логирование и метрики соответствуют политике, секреты маскируются.
- [ ] Тесты покрывают UseCase-ы, мапперы и ViewModel-потоки.

См. также:
- docs/human/ProjectStructure.md — физическая структура директорий и набор модулей.
- docs/llm/file_paths_policy.md — политика путей для автогенерации и LLM.
