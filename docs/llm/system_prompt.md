# AIChallenge — системный промпт для LLM

Этот документ описывает цели, задачи, инварианты и правила, по которым LLM должна анализировать репозиторий и генерировать изменения/артефакты. Основан на AIChallenge-StyleGuide.md и .clinerules/Project-rules.md. Проект: Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP), Чистая архитектура, MVI, Koin, Ktor, Coroutines, kotlinx.serialization.

Содержание:
- Контекст проекта
- Цели и не-цели
- Задачи для LLM (WBS с входами/выходами/критериями)
- Архитектурные инварианты и дизайн-правила
- Политики именования и путей
- Правила работы с данными, ошибками и логированием
- Правила для UI (MVI), навигации и DI
- Требования к результатам, формат вывода

## Контекст проекта

- Модули верхнего уровня:
  - composeApp — UI и навигация (Compose Multiplatform)
  - shared — общий код (Domain + Data + DI + утилиты)
  - server — Ktor-приложение
- Архитектура:
  - Слои: Presentation (UI/VM/State), Domain (use cases, domain models), Data (repos, sources, DTO)
  - Инверсия зависимостей, модульность по фичам, однонаправленный поток данных (MVI, StateFlow)
- KMP:
  - Максимум общего кода в commonMain
  - Платформенная специфика в androidMain/jvmMain и инжектируется через интерфейсы (DI)
- Документация:
  - docs/human — для разработчиков
  - docs/llm — для автогенерации

## Цели и не-цели

- Цели:
  - Генерация и поддержка документации и шаблонов согласно лучшим практикам KMP+CMP.
  - Генерация кода строго в рамках слоёв и фич-структуры, без смешения DTO в UI/Domain.
  - Автоматизация добавления фич и инфраструктурных улучшений без нарушения инвариантов.
- Не-цели:
  - Ломать границы слоёв, уводить платформенную специфику в commonMain, смешивать DTO и UI-модели.
  - Вносить изменения, противоречащие AIChallenge-StyleGuide.md и .clinerules/Project-rules.md.

## Задачи для LLM (Work Breakdown Structure)

Ниже — набор стандартных задач. При работе выбирай релевантные и следуй их входам/выходам и критериям.

T0. Инвентаризация кода
- Входы: дерево модулей (composeApp/shared/server), build-скрипты, текущая документация.
- Действия: собрать машинно-читабельную структуру модулей, фич, DI-модулей, основных интерфейсов и реализаций.
- Выход: docs/inventory/project_inventory.json.
- Критерии: полнота по всем модулям; корректные пути; указаны роли.

T1. Архитектура (human)
- Входы: T0, StyleGuide.
- Действия: обновить docs/human/Architecture.md (слои, MVI, KMP, DI, сеть, ошибки, логирование).
- Выход: согласованный документ с диаграммами (Mermaid).
- Критерии: нет противоречий StyleGuide; ссылки на код актуальны.

T2. Структура проекта (human)
- Входы: T0.
- Действия: описать модули, source sets, фич-структуру по шаблону {di,domain,presentation,data}.
- Выход: docs/human/ProjectStructure.md.
- Критерии: дерево путей, правила масштабирования, навигация.

T3. Стандарты кодирования (human)
- Входы: StyleGuide.
- Действия: расширить правила форматирования (ktlint/spotless), корутины (диспетчеры/скоупы), тестирование, commit/PR правила.
- Выходы: docs/human/CodingStandards.md, docs/human/TestingStrategy.md, docs/human/Contributing.md.
- Критерии: примеры кода, согласованность с фич-структурой.

T4. Введение новой фичи (human)
- Входы: T1–T3.
- Действия: пошаговое руководство и чек-лист DoD, шаблоны MVI и DI, навигация, мапперы DTO↔Domain.
- Выходы: docs/human/FeatureDevelopmentGuide.md, templates/feature/*.
- Критерии: шаблоны и шаги позволяют создать фичу без импровизации.

T5. Операционная документация (human)
- Входы: shared/data, server.
- Действия: сеть (Ktor), персистентность (DB/DataStore/кэш), ошибки, логирование/метрики, безопасность.
- Выходы: docs/human/API-Networking.md, Data-Persistence.md, ErrorHandling.md, Logging-Metrics.md, Security.md.
- Критерии: практичные примеры, рекомендации и политики.

T6. Вариант “для LLM”
- Входы: T1–T5.
- Действия: сжатые политики и шаблоны кода для автогенерации; инварианты; запрещённые операции.
- Выходы: docs/llm/code_generation_rules.md, file_paths_policy.md, knowledge/*, templates/*.kt.md, maintenance_rules.md.
- Критерии: 1–2 экрана базовых правил + модульные вложения; однозначность.

T7. Индекс и навигация
- Входы: T1–T6.
- Действия: docs/INDEX.md и ссылки из README на docs/human и docs/llm.
- Выходы: docs/INDEX.md, обновлённый README (при необходимости).
- Критерии: 2 клика до любого раздела, нет битых ссылок.

T8. Качество и поддержка
- Входы: T1–T7.
- Действия: чек-лист качества и политика версионирования docs, регламент обновления.
- Выходы: docs/human/DocumentationMaintenance.md, docs/llm/maintenance_rules.md.
- Критерии: наличие процесса обновления и ревью.

## Архитектурные инварианты и дизайн-правила

- Чистая архитектура. Слои независимы; Presentation знает только Domain-контракты, а реализации — в Data.
- MVI в UI. Однонаправленный поток: Event → VM → UseCase → Repo → New State (StateFlow).
- KMP. Общий код максимум в commonMain; платформенные части — в androidMain/jvmMain за интерфейсами.
- DI (Koin). Регистрация зависимостей на уровне интерфейсов; реализации биндятся в DI-модулях фич/слоёв.
- DTO-изоляция. DTO запрещены в Presentation и Domain; маппинг DTO↔Domain в Data.
- Ошибки. Низкоуровневые ошибки переводятся в DomainException; UI показывает user-friendly сообщения.
- Логирование. Уровни debug/info/error; контекстные записи; бизнес-события и ошибки фиксируются.

## Политики именования и путей

- Пакеты:
  - Базовый: `ru.izhxx.aichallenge`
  - Фича: `ru.izhxx.aichallenge.features.<feature>.<layer>`
- Структура фичи:
  - `features/<feature>/{di,domain,presentation,data}`
- Конвенции:
  - Классы: UpperCamelCase (ChatViewModel)
  - DTO: суффикс DTO (ChatMessageDTO)
  - Реализации интерфейсов: суффикс Impl (OpenAIApiImpl)
  - UseCase: глагол+существительное (SendMessageUseCase)
  - Константы: UPPER_SNAKE_CASE
- Размещение платформенной специфики: в соответствующих source sets (androidMain/jvmMain).
- Норматив по путям для автогенерации см. также docs/llm/file_paths_policy.md (если доступен).

## Правила работы с данными, ошибками и логированием

- Data слой:
  - API: Ktor + kotlinx.serialization (DTO в shared/src/commonMain/.../data/model)
  - Репозитории: изолируют источники данных, инкапсулируют маппинг DTO↔Domain.
  - Вызовы через safeApiCall, с преобразованием ошибок к DomainException.
- Ошибки:
  - RequestError/ApiError в data/error
  - DomainException в domain/model/error
- Логирование:
  - Использовать общий Logger (shared/common/Logger.kt)
  - Не логировать секреты; маскирование в сетевом логере.

## Правила для UI (MVI), навигации и DI

- UI:
  - ViewModel хранит MutableStateFlow<State>, наружу — StateFlow<State>.
  - Intent/Event — отдельные sealed-классы в presentation/model.
  - Эффекты (одноразовые) — отдельные каналы/SharedFlow при необходимости.
- Навигация:
  - Реализуется в composeApp App.kt; параметры маршрутов явно типизированы.
- DI:
  - DI-модули в features/<feature>/di и shared/di.
  - Инжекция VM через koinViewModel (Compose), другие зависимости — через get()/inject().

## Требования к результатам и формат вывода

- Изменения в коде:
  - Строго соблюдать размещение файлов по слоям/фичам и KMP source sets.
  - Публичные API документировать KDoc (RU).
  - Коррутины: явные диспетчеры, structured concurrency, корректные scope.
- Изменения в документации:
  - Markdown (RU), с оглавлением и перекрёстными ссылками, код-блоки с языком.
- Для больших задач:
  - Сначала предложить план изменений (diff/перечень файлов), затем реализовать частями.
- Тесты:
  - Unit для Domain/Data, snapshot/compose-testing для UI.
- Коммиты/PR:
  - Conventional Commits, чек-лист PR (линт, тесты, docs).

Коротко: генерируй изменения, соблюдая Чистую архитектуру, MVI, KMP, DI и StyleGuide. DTO в UI/Domain не допускаются, ошибки нормализуются, UI обновляется через StateFlow, DI — через Koin, сеть — через Ktor + safeApiCall, логирование и безопасность — обязательны.
