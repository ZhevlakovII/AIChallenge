# Структура проекта

Документ описывает физическую структуру директорий и модулей проекта AIChallenge для KMP+CMP. Согласован с Architecture.md, CodingStandards.md и .clinerules/Project-rules.md. За подробным списком файлов см. инвентаризацию: docs/inventory/project_inventory.json.

Содержание:
- Модули верхнего уровня
- Дерево директорий (сокращённо)
- KMP source sets
- Фича-ориентированная раскладка (features)
- Навигация и точки входа
- DI-модули
- Данные: API/DTO/БД/мапперы
- Тесты
- Соглашения по размещению
- Чек-лист соответствия

## Модули верхнего уровня

- composeApp — UI на Compose Multiplatform, навигация, ViewModel, экраны фич.
- shared — общий код Domain/Data/DI, модели, репозитории, API, БД, утилиты.
- server — серверная часть (Ktor), при необходимости.

## Дерево директорий (сокращённо)

```text
AIChallenge/
├─ composeApp/
│  ├─ src/
│  │  ├─ commonMain/
│  │  │  ├─ kotlin/ru/izhxx/aichallenge/
│  │  │  │  ├─ App.kt
│  │  │  │  ├─ di/AppModule.kt
│  │  │  │  └─ features/
│  │  │  │     ├─ chat/ { di, domain, presentation, data }
│  │  │  │     ├─ history/ { di, presentation, domain, data }
│  │  │  │     ├─ mcp/ { di, presentation }
│  │  │  │     ├─ metrics/ ChatMetricsScreen.kt
│  │  │  │     └─ settings/ { di, state, SettingsScreen.kt, SettingsViewModel.kt }
│  │  │  └─ composeResources/ (общие ресурсы Compose)
│  │  ├─ androidMain/
│  │  │  ├─ kotlin/ru/izhxx/aichallenge/{MainActivity.kt, MainApplication.kt}
│  │  │  └─ res/ (иконки/строки Android)
│  │  └─ jvmMain/
│  │     └─ kotlin/ru/izhxx/aichallenge/main.kt
│  └─ build.gradle.kts
│
├─ shared/
│  ├─ src/
│  │  ├─ commonMain/
│  │  │  └─ kotlin/ru/izhxx/aichallenge/
│  │  │     ├─ common/ { Constants.kt, Logger.kt, SafeApiCall.kt }
│  │  │     ├─ data/
│  │  │     │  ├─ api/ { OpenAIApi.kt, OpenAIApiImpl.kt }
│  │  │     │  ├─ database/ { AppDatabase.kt, DatabaseFactory.kt, dao/, entity/ }
│  │  │     │  ├─ error/ { ApiError.kt, RequestError.kt }
│  │  │     │  ├─ model/ { *DTO.kt }
│  │  │     │  ├─ parser/ { core/, impl/, LLMContentJsonModel.kt }
│  │  │     │  ├─ repository/ { *RepositoryImpl.kt }
│  │  │     │  ├─ service/ { *ServiceImpl.kt }
│  │  │     │  └─ usecase/ { *UseCaseImpl.kt }
│  │  │     ├─ di/ { SharedModule.kt, ParsersModule.kt, MetricsModule.kt, McpModule.kt, CompressionModule.kt, DataStoreProvider.kt }
│  │  │     ├─ domain/
│  │  │     │  ├─ model/ { config/, message/, response/, markdown/, error/ }
│  │  │     │  ├─ repository/ { *Repository.kt }
│  │  │     │  ├─ service/ { *Service.kt }
│  │  │     │  └─ usecase/ { *UseCase.kt }
│  │  │     └─ mcp/ { domain/, data/ }
│  │  ├─ androidMain/
│  │  │  └─ kotlin/ru/izhxx/aichallenge/di/DataStoreProvider.android.kt
│  │  └─ jvmMain/
│  │     └─ kotlin/ru/izhxx/aichallenge/di/DataStoreProvider.jvm.kt
│  └─ build.gradle.kts
│
├─ server/
│  ├─ src/main/kotlin/ru/izhxx/aichallenge/Application.kt
│  ├─ src/main/resources/logback.xml
│  └─ src/test/kotlin/ru/izhxx/aichallenge/ApplicationTest.kt
│
├─ docs/
│  ├─ human/ { Architecture.md, ProjectStructure.md, CodingStandards.md, FeatureDevelopmentGuide.md, ... }
│  ├─ llm/ { system_prompt.md, code_generation_rules.md, file_paths_policy.md, maintenance_rules.md, knowledge/, templates/ }
│  └─ inventory/project_inventory.json
└─ settings.gradle.kts, build.gradle.kts, gradle/, README.md
```

## KMP source sets

- Общий код: `*/src/commonMain/kotlin` и `*/src/commonTest/kotlin`
- Android-специфика: `*/src/androidMain/kotlin` (+ ресурсы в `composeApp/src/androidMain/res`)
- JVM/desktop: `*/src/jvmMain/kotlin`

Правила:
- Максимум логики — в `commonMain`.
- Платформенные реализации — за интерфейсами, инжектируются через DI.
- В `commonMain` не допускаются платформенные типы/классы.

## Фича-ориентированная раскладка (features)

Каждая фича в UI-модуле (composeApp) имеет подпакеты:
```
features/<feature>/
  ├─ di/
  ├─ domain/ (model/, usecase/)
  ├─ presentation/
  │  ├─ <Feature>Screen.kt
  │  ├─ <Feature>ViewModel.kt
  │  └─ model/ { <Feature>State.kt, <Feature>Event.kt }
  └─ data/ (по необходимости)
```
- Общие реализации и повторно используемые репозитории — в `shared`.
- Навигация регистрирует маршрут фичи в `App.kt`.

## Навигация и точки входа

- Точка входа приложения: `composeApp/src/commonMain/.../App.kt`
- Навигация: `sealed class Screen`, `NavHost` со стартовым экраном; переходы — через параметры маршрута и колбэки из UI.
- Android-старт: `MainActivity.kt`, `MainApplication.kt` (инициализация DI и т.д.).
- Desktop/JVM: `main.kt`.

## DI-модули

- Общие DI: `shared/src/commonMain/.../di/*.kt` (SharedModule, ParsersModule, MetricsModule, McpModule, CompressionModule).
- Фичевые DI: `composeApp/.../features/<feature>/di/<Feature>Module.kt`.
- Провайдеры платформенных сервисов: `DataStoreProvider` в `shared/di` + реализации в androidMain/jvmMain.

## Данные: API/DTO/БД/мапперы

- API-клиенты: `shared/.../data/api/*` (интерфейсы + Impl), настройка Ktor — через DI.
- DTO: `shared/.../data/model/*DTO.kt` — только в Data-слое.
- БД: `shared/.../data/database/{dao,entity}` + `AppDatabase.kt`, `DatabaseFactory.kt`.
- Мапперы: extension-функции рядом с реализациями (Data), преобразуют DTO/Entity ↔ Domain.
- Ошибки: `data/error/*` (низкоуровневые) и `domain/model/error/DomainException.kt` (доменная).

## Тесты

- Общие юнит-тесты: `shared/src/commonTest/kotlin` (Domain/Data, мапперы, usecase, репозитории).
- UI/ViewModel тесты: `composeApp/src/commonTest/kotlin`.
- Server-тесты: `server/src/test/kotlin`.
- Рекомендации по тестам: см. docs/human/TestingStrategy.md.

## Соглашения по размещению

- DTO никогда не попадают в Presentation/Domain.
- ViewModel/State/Event — в `presentation`, экран — `<Feature>Screen.kt`.
- UseCase интерфейсы — в Domain, реализации — либо в фиче (если локальны), либо в shared.
- Репозитории: контракты — в Domain, реализации — в Data (shared).
- Навигация централизована в `App.kt`.
- Подписка в UI на состояние: `collectAsStateWithLifecycle()`.

## Чек-лист соответствия

- [ ] Файлы и каталоги соответствуют шаблону features.
- [ ] Общие реализации помещены в `shared`.
- [ ] Навигация обновлена для новых экранов.
- [ ] DI-модуль фичи создан и подключён.
- [ ] DTO/Entity не пересекают границы в Presentation/Domain.
- [ ] Тестовые файлы размещены в `*/src/commonTest/kotlin`.

Ссылки:
- Architecture.md — логическая архитектура.
- docs/llm/file_paths_policy.md — политика путей (LLM).
- docs/inventory/project_inventory.json — актуальный список ключевых файлов.
