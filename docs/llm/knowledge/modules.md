# Модули и фичи (сводка для LLM)

Краткая справка по модулям и фичам проекта, с указанием ролей и ключевых путей. Подробно см.: docs/human/ProjectStructure.md, docs/human/Architecture.md, docs/llm/file_paths_policy.md. Полный список файлов: docs/inventory/project_inventory.json.

Содержание:
- Модули верхнего уровня
- Фичи UI (Compose)
- Общие DI-модули и провайдеры
- Навигация
- Быстрые инварианты (LLM)

## Модули верхнего уровня

- composeApp — UI-модуль на Compose Multiplatform:
  - Раскладка фич: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/{di,domain,presentation,data}`
  - Навигация и App: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/App.kt`
  - Android entry: `composeApp/src/androidMain/kotlin/.../MainActivity.kt`, `MainApplication.kt`
  - Desktop/JVM entry: `composeApp/src/jvmMain/kotlin/.../main.kt`

- shared — общий код Domain/Data/DI:
  - Domain: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/**`
  - Data (репозитории, API, БД, DTO): `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/**`
  - Общие DI: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/**`
  - Общие утилиты: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/**`
  - Платформенные адаптеры: `shared/src/androidMain/**`, `shared/src/jvmMain/**`

- server — серверная часть (Ktor) (опционально): `server/src/main/**`

## Фичи UI (Compose)

Примеры текущих фич (актуальный список см. inventory):
- chat:
  - Base: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/chat`
  - DI: `features/chat/di/ChatModule.kt`
  - Presentation: экран, VM, компоненты, модели UI
  - Domain/usecase (локальные для фичи): `features/chat/domain/usecase/**` (если есть)
- settings:
  - Base: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/settings`
  - DI: `features/settings/di/SettingsModule.kt`
  - Presentation: `SettingsScreen.kt`, `SettingsViewModel.kt`, `state/SettingsState.kt`
- metrics:
  - Base: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/metrics`
  - Presentation: `ChatMetricsScreen.kt`
- mcp:
  - Base: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/mcp`
  - DI: `features/mcp/di/McpModule.kt`
  - Presentation: экран/VM/состояния MCP
- history (запланирована/скэффолджена): `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/history`

Фича-шаблон:
```
features/<feature>/
  ├─ di/<Feature>Module.kt
  ├─ domain/(model|usecase)
  ├─ presentation/
  │  ├─ <Feature>Screen.kt
  │  ├─ <Feature>ViewModel.kt
  │  └─ model/{<Feature>State.kt, <Feature>Event.kt}
  └─ data/ (по необходимости)
```

## Общие DI-модули и провайдеры

- Общие DI:
  - `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/SharedModule.kt`
  - `.../ParsersModule.kt`, `MetricsModule.kt`, `McpModule.kt`, `CompressionModule.kt`
  - Провайдеры конфигов/хранилищ: `DataStoreProvider.kt` (+ platform в androidMain/jvmMain)

- Политика регистрации:
  - Интерфейсы (Domain) → реализации (Data) регистрируются в DI.
  - ViewModel и UseCase фич — в `<Feature>Module.kt`.

## Навигация

- Входная точка и граф: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/App.kt`
- Экран фичи добавляется в NavHost через `composable(Screen.<Feature>.route) { <Feature>Screen(...) }`
- Аргументы маршрутов — через параметры route/`navArgument`.

## Быстрые инварианты (LLM)

- Не выводить DTO за пределы Data; маппинг в Domain — в Data-слое (extension-функции).
- Presentation зависит только от Domain-контрактов/UseCase-ов.
- Использовать `collectAsStateWithLifecycle()` в UI.
- Все внешние IO/сеть оборачивать в `safeApiCall { ... }`, конвертировать ошибки в `DomainException`.
- DI: зависимости во ViewModel только через Koin, без прямых `new`.

Ссылки:
- Полный список путей: docs/inventory/project_inventory.json
- Архитектура: docs/human/Architecture.md
- Структура: docs/human/ProjectStructure.md
- Политика путей: docs/llm/file_paths_policy.md
