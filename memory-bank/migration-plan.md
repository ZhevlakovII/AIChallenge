# План миграции на мультимодульную архитектуру (ADR-2025-11-21)

Документ описывает целевую архитектуру, инварианты, этапы миграции (Stage 0 → 5), артефакты, критерии приёмки (DoD) и контроль зависимостей. Базируется на ADR-2025-11-21 и текущем состоянии репозитория (модули: :composeApp, :shared, :server).

Сопутствующие документы:
- Stage-0: memory-bank/stage-plans/Stage-0-Plan.md
- Стиль разработки/архитектура: docs/human/*
- Трассируемость: memory-bank/activeContext.md, sessions/, progress.md

## 1. Целевые принципы (инварианты)

- Kotlin Multiplatform (Android/JVM) с Compose Multiplatform.
- Единый Java Toolchain 21 (все модули).
- Версии и плагины централизованы в gradle/libs.versions.toml (versions catalog).
- DI: Koin. Границы DI совпадают с границами Gradle-модулей. Композиция — в :instances (root-агрегатор, планируется к добавлению).
- Сеть: Ktor client/server, kotlinx.serialization. Общие контракты — в :shared:contracts.
- Persistence: Room KMP (commonMain), KSP multi-target; sqlite-bundled на JVM; миграции схем под контролем VCS.
- Навигация: контракты в :frontend:core:navigation, реализации в :frontend:features/<feature>:{api,impl}.
- Ошибки: DomainException — :shared:core:exceptions; SafeApiCall — :frontend:core:common.
- Логи/метрики: API (:shared:core:logging, :shared:core:metrics) и платформенные реализации отдельно (:androidImpl, :jvmImpl).
- Контроль зависимостей: jdeps + Graphviz. Жёсткое запрещение горизонтальных зависимостей между features и запрещённых направлений слоёв.
- Gradle-конвенции: Java Toolchain 21, versions catalog, convention plugins, TYPESAFE_PROJECT_ACCESSORS, Foojay resolver.

## 2. Целевая модульная структура (итеративное формирование)

- :composeApp — entrypoint UI (Android/JVM).
- :instances — DI-агрегатор (root-композиция графа).
- :frontend:core:common — общие UI utils (SafeApiCall, base-вьюмодели и т.д.).
- :frontend:core:navigation — контракты/маршруты.
- :frontend:features:<feature>:api — публичные контракты фичи (интерфейсы, модели UI-уровня).
- :frontend:features:<feature>:impl — реализация фичи (исполнение api, зависимости из :shared и core).
- :shared:core:exceptions — исключения domain-уровня (DomainException и т.п.).
- :shared:core:logging, :shared:core:metrics — абстракции логов/метрик.
- :shared:contracts — сетевые/доменные контракты, DTO ↔ Domain маппинги.
- :shared:domain — use cases, сущности, интерфейсы репозиториев.
- :shared:data — реализации репозиториев, источники (network/db), мапперы.
- :shared:platform:<target> — платформенные реализации абстракций (logging/metrics/storage и пр.).
- :server — Ktor server (изолированная JVM-компонента).

Примечание: исходный :shared будет декомпозирован на перечисленные подмодули постепенно.

## 3. Правила зависимостей (слои)

- presentation (composeApp, frontend:core, frontend:features:*:*impl) -> domain (shared:domain) -> data (shared:data).
- DTO недопустимы в presentation/domain (только contracts/data).
- Запрещено:
  - Presentation -> Data напрямую.
  - Domain -> Data.
  - Domain <-> Presentation в обе стороны.
  - Любые горизонтальные зависимости между features (feature A impl не видит feature B impl).
- Разрешено:
  - composeApp зависит от feature APIs (+ некоторых core).
  - feature impl зависит от своего feature api, shared:domain, shared:data, core.
  - shared:data зависит от shared:contracts и platform-слоёв.

## 4. Контроль зависимостей (jdeps/Graphviz)

- Генерация DOT графов: :composeApp, :shared, :server (уже есть задачи jdeps*).
- Копирование в docs/architecture/<module>/ и рендер SVG при наличии Graphviz.
- Встраивание раскраски слоёв и маркировка запрещённых рёбер (FORBIDDEN).
- Эскалация строгости:
  - Stage 0–2: отчёт + визуализация, не валит билд.
  - Stage 3+: при наличии нарушений — build fail.

## 5. Этапы миграции

### Stage 0 — Инфраструктура и консистентность
Цель: единая toolchain (Java 21), выверка versions catalog, совместимость Kotlin/KSP, активация и проверка jdeps pipeline, подготовка convention plugins (скелет).
- Действия: см. memory-bank/stage-plans/Stage-0-Plan.md.
- DoD:
  - Все модули собираются на Java 21.
  - КСП версия совместима с Kotlin (проверено/зафиксировано).
  - jdeps-графы генерируются для :composeApp, :shared, :server, артефакты в docs/architecture.
  - Обновлены memory-bank/activeContext.md, progress.md.

### Stage 1 — Базовая декомпозиция shared
Цель: отделить shared:core:exceptions, shared:contracts, shared:domain (минимальный).
- Действия:
  - Создать модули, перенести DomainException и др. исключения.
  - Вынести контракты (DTO/модели/интерфейсы).
  - Вынести базовые use cases/сущности в shared:domain (только интерфейсы репозиториев).
- DoD:
  - :composeApp и :server собираются без регрессий.
  - jdeps показывает отсутствие DTO в presentation/domain.

### Stage 2 — Data слой
Цель: создать shared:data, перенести реализации репозиториев, источников и мапперов. Подготовить Room KMP/KSP multi-target конфигурации.
- Действия:
  - Настроить KSP per-target (android/jvm), schemaDirectory под VCS.
  - Вынести реализации в data, domain остаётся без зависимостей на data.
- DoD:
  - Unit-тесты репозиториев.
  - jdeps без запрещённых рёбер; миграции схем под контролем.

### Stage 3 — Frontend границы и запреты
Цель: ввести frontend:core и features :api/:impl для существующих фич (chat, history, mcp, metrics, reminder, settings).
- Действия:
  - Выделить навигационные контракты в core.
  - Для каждой фичи создать api/impl; composeApp зависит от api.
  - Включить build fail при нарушениях зависимостей (jdeps-проверка).
- DoD:
  - Горизонтальные зависимости между фичами отсутствуют.
  - Запрещённые рёбра валят билд.

### Stage 4 — Платформенные реализации
Цель: разнести платформенные реализации logging/metrics/storage в shared:platform:<target>.
- DoD:
  - Вся платформа инкапсулирована; domain/data не знают о target-конкретике.

### Stage 5 — Укрепление, перформанс, тесты
Цель: нагрузка на jdeps/Graphviz/трассируемость, покрытие тестами, перформанс-тюнинг.
- DoD:
  - Покрытие unit/интеграционных тестов.
  - Стабильные графы зависимостей, отсутствие нарушений.

## 6. Совместимость и версии (матрица)

- Java Toolchain: 21 (везде).
- Kotlin: см. libs.versions.toml (текущая: 2.2.21).
- KSP: должен соответствовать Kotlin. Действие Stage 0: выровнять KSP до совместимой версии или повысить Kotlin (фиксировать выбор в ADR/ChangeLog).
- Compose Multiplatform, Ktor, Room — как в catalog; при обновлениях — через PR и changelog.

## 7. Метрики прогресса и артефакты

- Артефакты:
  - docs/architecture/<module>/summary.dot, summary.decorated.dot, *.svg, violations.txt
  - memory-bank/stage-plans/Stage-*-Plan.md
  - memory-bank/progress.md — обновляем после каждого Stage
- Метрики:
  - Кол-во запрещённых рёбер (должно уменьшаться → 0 к Stage 3).
  - Успешные сборки всех таргетов.
  - Покрытие тестами для shared:data/domain/features.

## 8. Риски и смягчение

- Несоответствие Kotlin/KSP — решение в Stage 0 (проверка и выравнивание).
- Неустановленный Graphviz — визуализация опциональна, отчёты DOT доступны.
- Потенциальная регрессия при декомпозиции — feature flags, поэтапные PR, CI.

## 9. Процесс и PR-пакеты

- Каждому Stage соответствует отдельный PR/пакет правок.
- CHANGELOG включает:
  - Изменённые модули/версии.
  - Влияние на API/контракты.
  - Шаги миграции (если требуются).
