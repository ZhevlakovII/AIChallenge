# Политика путей и структуры файлов (LLM)

Документ нормирует размещение файлов и модулей для автогенерации кода. Обязателен для соблюдения Чистой архитектуры, KMP и фич-ориентированной структуры. Базируется на AIChallenge-StyleGuide.md и .clinerules/Project-rules.md.

Содержание:
- Модули верхнего уровня
- KMP source sets и роль каждого
- Раскладка по слоям (Presentation / Domain / Data)
- Структура фич
- Именование файлов и классов
- Специальные директории и исключения
- Быстрые рецепты автогенерации

## Модули верхнего уровня

- UI (Compose MPP): `composeApp/`
- Общий код (Domain/Data/DI/утилиты): `shared/`
- Сервер (Ktor): `server/`

Gradle:
- Корень: `build.gradle.kts`, `settings.gradle.kts`
- Версии: `gradle/libs.versions.toml`

## KMP source sets

- Общий код: `*/src/commonMain/kotlin`
- Android-специфика: `*/src/androidMain/kotlin` (+ ресурсы в `composeApp/src/androidMain/res`)
- JVM/desktop-специфика: `*/src/jvmMain/kotlin`
- Общие тесты: `*/src/commonTest/kotlin`

Правила:
- Максимально возможная доля логики — в `commonMain`.
- Платформенные реализации инкапсулировать за интерфейсами и инжектировать через DI.
- Не размещать платформенно-специфичные типы в `commonMain`.

## Раскладка по слоям

- Presentation (UI, ViewModel, MVI):
  - `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/presentation/**`
- Domain (контракты, модели, use cases):
  - Общие контракты/модели: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/**`
  - Фичевые use case (если локальны фиче): `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/domain/**`
- Data (репозитории, источники, DTO, мапперы):
  - Общие реализации и модели: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/**`
  - Если реализация строго фичезависима — допускается `features/<feature>/data/**` в composeApp (предпочтительно общие реализации хранить в shared).

DI:
- Общие DI-модули: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/**`
- DI фич: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/di/**`

## Структура фич

Базовый шаблон:
```
composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/
  ├── di/
  │   └── <Feature>Module.kt
  ├── domain/
  │   ├── model/                 // при необходимости
  │   └── usecase/               // интерфейсы и реализации UseCase
  ├── presentation/
  │   ├── <Feature>Screen.kt
  │   ├── <Feature>ViewModel.kt
  │   ├── model/
  │   │   ├── <Feature>State.kt
  │   │   └── <Feature>Event.kt
  │   └── components/            // UI-компоненты
  └── data/                      // при необходимости: репозитории/источники/мапперы/DTO
```

Навигация:
- Общая точка: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/App.kt` (sealed `Screen`, `NavHost`).

## Именование файлов и классов

- ViewModel: `<Feature>ViewModel.kt`, класс `<Feature>ViewModel`.
- Экран: `<Feature>Screen.kt`, функция `@Composable fun <Feature>Screen(...)`.
- Состояние/События: `model/<Feature>State.kt`, `model/<Feature>Event.kt`.
- UseCase: `<Action><Entity>UseCase(.kt)`, реализация `<Action><Entity>UseCaseImpl`.
- Репозиторий (контракт): `<Entity>Repository.kt`, реализация `<Entity>RepositoryImpl.kt`.
- DTO: `<Entity>DTO.kt` (только Data-слой).
- DI-модуль: `<Feature>Module.kt`, объект/val `<feature>Module`.

## Специальные директории и исключения

- Общие утилиты: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/**`
  - `Logger.kt`, `Constants.kt`, `SafeApiCall.kt`
- Сеть и модели: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/api/**`, `.../data/model/**`
- Ошибки: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/error/**`, доменные — `.../domain/model/error/**`
- БД/DAO: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/database/**`, платформенные фабрики — в `androidMain`/`jvmMain`.

## Быстрые рецепты автогенерации

1) Новая фича:
- Создать каталоги по шаблону в `features/<feature>/...`
- Сгенерировать State/Event/ViewModel/Screen в `presentation`
- Добавить UseCase интерфейсы и реализации в `domain/usecase`
- Если нужны данные — `data/` с репозиторием и мапперами DTO↔Domain (или использовать общий репозиторий из `shared`)
- Создать `di/<Feature>Module.kt` и зарегистрировать зависимости
- Добавить маршрут в `App.kt`

2) Новый UseCase (общий):
- Путь: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/usecase/<Action><Entity>UseCase.kt`
- Реализация: `.../<Action><Entity>UseCaseImpl.kt`
- Регистрация: общий DI-модуль или модуль фичи

3) Новый DTO и API:
- DTO: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/model/<Entity>DTO.kt`
- API: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/api/<ApiName>.kt`
- Реализация API: `<ApiName>Impl.kt`
- Репозиторий: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/repository/<Entity>RepositoryImpl.kt`
- Маппер: рядом с реализацией (extension-функции)

4) Платформенные адаптеры:
- Android/JVM реализации — в соответствующих `*/src/<platform>Main/kotlin/**`
- Подключать через DI, не выносить платформенные классы в `commonMain`

Инварианты:
- DTO не пересекает границу Presentation/Domain.
- Навигация централизована в `App.kt`.
- Коррутины: `safeApiCall` и конверсия ошибок в `DomainException` на границе Data→Domain.
