# Глоссарий проекта

Обновлено: 2025-11-22  
Язык: RU  
Статус: Active

Связанные документы:
- Product: [productContext.md](./productContext.md)
- Tech: [techContext.md](./techContext.md)
- Паттерны: [systemPatterns.md](./systemPatterns.md)
- Решения (ADR): [decisions/](./decisions/)
- Принятый ADR миграции KMP: [decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md)
- Сессии: [sessions/](./sessions/)
- Прогресс: [progress.md](./progress.md)
- Project Brief: [./projectbrief.md](./projectbrief.md)
- Руководства: [../docs/human/](../docs/human/), Политики LLM: [../docs/llm/](../docs/llm/)

---

Назначение  
Единый словарь терминов, аббревиатур и конвенций проекта (SSoT), обеспечивающий общее понимание между участниками и трассируемость с ADR, паттернами и сессиями.

Правила ведения
- Язык — RU; даты — YYYY-MM-DD.
- Алфавитный порядок по русской транслитерации/орфографии термина.
- При значимых изменениях терминов — оформить ADR (см. [./_templates/ADR-template.md](./_templates/ADR-template.md)) и синхронизировать [systemPatterns.md](./systemPatterns.md).
- Ссылки на источники контекста обязательны, если термин влияет на архитектурные решения.

---

Содержание

- ADR (Architectural Decision Record)
  - Определение: Формализованная запись архитектурного решения, его мотивации, альтернатив и последствий.
  - Статусы: Proposed → Accepted/Rejected → Superseded.
  - Ссылки: [decisions/](./decisions/), [systemPatterns.md](./systemPatterns.md), [sessions/](./sessions/), [progress.md](./progress.md).
  - Трассируемость: См. принятый ADR миграции KMP — [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Active Context (Оперативный контекст)
  - Определение: Сводка текущих целей, приоритетов и активных задач.
  - Ссылки: [activeContext.md](./activeContext.md).

- Backend Core (backend:core:*)
  - Определение: Базовые серверные модули (конфигурация Ktor, сериализация, DI, инфраструктура).
  - Зависимости: Может зависеть от `:shared:core:*` и `:shared:contracts:*`. Не зависит от фронтенда.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Backend Feature API/Impl (backend:features:<feature>:{api,impl})
  - Определение: Контракты (api) и реализации (impl) серверных фич.
  - Правила: `impl` зависит от `api`, `backend:core:*`, `:shared:*`; горизонтальные зависимости между фичами запрещены.
  - Синхронизация моделей: Совместимы c `:shared:contracts:*`.
  - Паттерны: SP-001 (Топология модулей KMP и инварианты зависимостей) — см. [systemPatterns.md](./systemPatterns.md).
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Bridge / Оркестратор
  - Определение: Межфичевый слой координации взаимодействий между независимыми фичами через их API без прямых горизонтальных зависимостей.
  - Назначение: Снижение связности, соблюдение инварианта запрета горизонтальных зависимостей.
  - Ссылки: [systemPatterns.md](./systemPatterns.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Compose Multiplatform
  - Определение: UI-технология для мультиплатформенной разработки на Kotlin.
  - Контекст: Исторически модуль `composeApp`; после миграции — UI в `:frontend:*`.
  - Ссылки: [techContext.md](./techContext.md), [systemPatterns.md](./systemPatterns.md).

- Convention Plugins (aichallenge.library / aichallenge.compose / aichallenge.server)
  - Определение: Внутренние Gradle-конвенции для унифицированной настройки библиотек KMP, Compose и серверных модулей.
  - Назначение: Единообразная конфигурация, снижение дублирования.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Contracts (shared:contracts:*)
  - Определение: Единые контракты данных/DTO, ошибок, роутинга и версионирования, разделяемые фронтом и бэком.
  - Правила: Только `commonMain`; без платформенных и UI-зависимостей. Источник истины для сетевых моделей.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Core (shared:core:*)
  - Определение: Общие базовые абстракции и утилиты (константы, ошибки, сериализация, value-объекты).
  - Правила: Не зависит от фич и UI.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- DataStore / Persistence
  - Определение: Механизмы локального хранения (preferences/DB).
  - Ограничение: DataStore используется только во фронтенд-модулях при необходимости.
  - Ссылки: [docs/human/Data-Persistence.md](../docs/human/Data-Persistence.md), [techContext.md](./techContext.md).

- DI (Dependency Injection) / Koin
  - Определение: Контейнер внедрения зависимостей; Koin — DI-фреймворк.
  - Правило: Границы DI-модулей совпадают с границами Gradle-модулей; root-агрегатор для конечных инстансов.
  - Ссылки: [techContext.md](./techContext.md), [systemPatterns.md](./systemPatterns.md).

- DoD (Definition of Done)
  - Определение: Набор проверяемых критериев готовности этапа/фичи.
  - Контекст: Определён по этапам миграции 0→5.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md), [progress.md](./progress.md).

- DomainException
  - Определение: Единая иерархия доменных ошибок проекта.
  - Расположение: `:shared:core:exceptions`.
  - Назначение: Консистентная обработка ошибок во всех слоях.
  - Паттерны: SP-030 (Безопасные вызовы и типизированные ошибки) — см. [systemPatterns.md](./systemPatterns.md).
  - Ссылки: [docs/human/ErrorHandling.md](../docs/human/ErrorHandling.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- DTO / Domain Model / Mapper
  - Определение: DTO — транспортные модели; Domain — доменные сущности; Mapper — преобразование между слоями.
  - Ссылки: [systemPatterns.md](./systemPatterns.md).

- Frontend Core (frontend:core:*)
  - Определение: Базовые фронтовые слои (навигация-контракты, UI темы, platform adapters, DI-агрегаторы).
  - Правила: Не содержит реализаций конкретных фич.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Frontend Feature API/Impl (frontend:features:<feature>:{api,impl})
  - Определение: `api` — внешние контракты фичи (модели, интерфейсы use-case, точки входа/навигация); `impl` — реализация (UI, ViewModel, репозитории).
  - Правила: `impl` зависит от `api`, `frontend:core:*`, `:shared:*`; горизонтальные зависимости между фичами запрещены (общение через API и/или мост/оркестратор).
  - Паттерны: SP-001 (Топология модулей KMP и инварианты зависимостей) — см. [systemPatterns.md](./systemPatterns.md).
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Graphviz (диаграммы зависимостей)
  - Определение: Инструмент визуализации графов зависимостей (DOT).
  - Контекст: Используется совместно с jdeps для отчётов.
  - Ссылки: [docs/human/ProjectStructure.md](../docs/human/ProjectStructure.md).

- Instances (instances:frontend:<platform> / instances:servers:<server>)
  - Определение: Сборочные инстансы приложений и серверов, отвечающие за точки входа, wire-up DI и агрегацию модулей.
  - Правила: Зависит от `api/impl` фич, `core`, `:shared:*`; не экспортирует реализацию наружу.
  - Примеры: `:instances:frontend:android`, `:instances:frontend:desktop`, `:instances:servers:mcp`.

- Java Toolchain 21
  - Определение: Единый toolchain Java = 21 для всех модулей (KMP/Server/Desktop).
  - Назначение: Консистентность сборки и бинарной совместимости.
  - Ссылки: [gradle/libs.versions.toml](../gradle/libs.versions.toml), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- jdeps (контроль зависимостей)
  - Определение: Анализатор зависимостей модулей/артефактов.
  - Политика: Визуализация через DOT/Graphviz; build в режиме fail при нарушениях инвариантов после DoD Этапа 3.
  - Ссылки: [docs/human/ProjectStructure.md](../docs/human/ProjectStructure.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- KMP (Kotlin Multiplatform)
  - Определение: Подход для общего кода между платформами (commonMain) с платформенными реализациями.
  - Текущие модули до миграции: `composeApp/`, `shared/`, `server/`. Целевая топология — см. ADR (модули `:shared`, `:frontend`, `:backend`, `:instances`).
  - Ссылки: [techContext.md](./techContext.md), [productContext.md](./productContext.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Ktor (Client/Server)
  - Определение: Сетевой стек (HTTP клиент/сервер) на Kotlin.
  - Контекст: Клиент — фронт; сервер — бэк; общие DTO — в `:shared:contracts:*`.
  - Ссылки: [techContext.md](./techContext.md), [../docs/human/API-Networking.md](../docs/human/API-Networking.md).

- Logger / Логирование
  - Определение: Механизм записи диагностической информации.
  - Ссылки: [docs/human/Logging-Metrics.md](../docs/human/Logging-Metrics.md), [systemPatterns.md](./systemPatterns.md).

- MCP (Model Context Protocol)
  - Определение: Протокол интеграции инструментов/контекстов ИИ.
  - Контекст: Интеграции и политики использования в проекте.
  - Ссылки: [techContext.md](./techContext.md), [../docs/llm/](../docs/llm/).

- Metrics / Метрики
  - Определение: Сбор и анализ метрик производительности/качества.
  - Ссылки: [docs/human/Logging-Metrics.md](../docs/human/Logging-Metrics.md), [systemPatterns.md](./systemPatterns.md).

- Navigation Contracts (frontend:core:navigation)
  - Определение: Контракты навигации/роуты без UI-реализаций.
  - Правила: Экспонируют точки входа фич через `:api`, UI находится в `:impl`.
  - Ссылки: [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Progress Log (Журнал прогресса)
  - Определение: Хронологический журнал изменений и результатов; обратная хронология (новое сверху).
  - Ссылки: [progress.md](./progress.md), [sessions/](./sessions/).

- Repository (Репозиторий)
  - Определение: Слой доступа к данным, инкапсулирующий источники (сеть, БД).
  - Ссылки: [systemPatterns.md](./systemPatterns.md), [docs/human/Data-Persistence.md](../docs/human/Data-Persistence.md).

- Room KMP
  - Определение: ORM/БД-абстракция с поддержкой KMP.
  - Правила: Только в `commonMain`; KSP многотаргетная (`kspCommonMainMetadata`, `kspAndroid`, `kspJvm` и т.д.); на JVM — `sqlite-bundled`; схемы миграций в `module/schemas`.
  - Ссылки: [docs/human/Data-Persistence.md](../docs/human/Data-Persistence.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- SafeApiCall
  - Определение: Обёртка для безопасного сетевого вызова с обработкой ошибок.
  - Контекст: `:frontend:core:common` (перенесено из монолита `shared`).
  - Паттерны: SP-030 (Безопасные вызовы и типизированные ошибки) — см. [systemPatterns.md](./systemPatterns.md).
  - Ссылки: [techContext.md](./techContext.md), [docs/human/ErrorHandling.md](../docs/human/ErrorHandling.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Secrets / Конфигурации окружений
  - Определение: Управление секретами и параметрами окружений (dev/stage/prod).
  - Ссылки: [docs/human/Security.md](../docs/human/Security.md), [systemPatterns.md](./systemPatterns.md).

- Session Note (Заметка сессии)
  - Определение: Документ с целями, ходом и итогами конкретной рабочей сессии.
  - Ссылки: [sessions/](./sessions/), [./_templates/session-template.md](./_templates/session-template.md).

- SP (System Pattern, SP-XXX)
  - Определение: Каталогизированный паттерн/конвенция системы с идентификатором SP-XXX.
  - Жизненный цикл: Draft → Proposed (ADR) → Accepted/Rejected → Superseded.
  - Ссылки: [systemPatterns.md](./systemPatterns.md), [decisions/](./decisions/).

- Special Router (Специальный роутер сервера)
  - Определение: Центральный роутер серверных эндпоинтов с привязкой к версиям контрактов из `:shared:contracts:*`.
  - Назначение: Декуплинг маршрутизации от реализаций фич; контроль версионирования путей.
  - Ссылки: [../docs/human/API-Networking.md](../docs/human/API-Networking.md), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- SSoT (Single Source of Truth, Единый источник истины)
  - Определение: Принцип единой консистентной правды о контексте/правилах проекта.
  - Ссылки: [productContext.md](./productContext.md), [techContext.md](./techContext.md), [activeContext.md](./activeContext.md).

- Typesafe Project Accessors
  - Определение: Gradle-фича `TYPESAFE_PROJECT_ACCESSORS` для обращения к проектам через `projects.*`.
  - Назначение: Безошибочная навигация по модулям, повышение надёжности конфигурации.
  - Ссылки: [settings.gradle.kts](../settings.gradle.kts), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Use Case (Прецедент/Интерактор)
  - Определение: Прикладная операция доменной логики с чёткой ответственностью.
  - Ссылки: [docs/human/Architecture.md](../docs/human/Architecture.md), [systemPatterns.md](./systemPatterns.md).

- Versions Catalog (libs.versions.toml)
  - Определение: Централизованный каталог версий зависимостей/плагинов Gradle.
  - Назначение: Единые версии Kotlin/Compose/AGP/Ktor/Room и др.
  - Ссылки: [../gradle/libs.versions.toml](../gradle/libs.versions.toml), [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

- Верификация структуры
  - Определение: Регулярная проверка целостности каталогов/файлов памяти.
  - Ссылки: [progress.md](./progress.md), [sessions/](./sessions/).

---

Процесс изменений и трассируемость
- Любое изменение терминов, влияющее на архитектуру/процессы, инициировать ADR.
- После принятия ADR обновить связанные статьи: [systemPatterns.md](./systemPatterns.md), [techContext.md](./techContext.md), ссылки в [sessions/](./sessions/) и [progress.md](./progress.md).
- В progress.md фиксировать факт обновления глоссария с указанием ADR.
- Кросс-ссылки: см. [ADR-2025-11-21…](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md).

История изменений
- 2025-11-22: Обновлено по принятому ADR миграции KMP: добавлены термины (Instances, Contracts, Core, Frontend/Backend Core, Features API/Impl, Bridge/Оркестратор, Special Router, Room KMP, jdeps/Graphviz, Convention Plugins, Java Toolchain 21, Versions Catalog, Typesafe Project Accessors, DoD, Navigation Contracts); контекст SafeApiCall перенесён в `:frontend:core:common`; добавлены кросс-ссылки на ADR.
- 2025-11-21: Создан начальный глоссарий; добавлены базовые термины (ADR, SSoT, KMP, Koin, Ktor, MCP, SP-XXX, SafeApiCall, Repository, Use Case, Session Note, Progress Log, Logger/Metrics, Secrets/Configs).
