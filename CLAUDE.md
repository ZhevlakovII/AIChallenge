# CLAUDE.md

Этот файл предоставляет руководство для Claude Code (claude.ai/code) при работе с кодом в этом репозитории.

## Обзор проекта

Это **Kotlin Multiplatform** проект, реализующий приложение AI Challenge с поддержкой платформ Android,
iOS, Desktop (JVM) и CLI.
Кодовая база использует современные практики Kotlin с Compose Multiplatform для UI,
Ktor для сетевых операций и следует принципам Clean Architecture с паттерном MVI.

**Основные принципы:** SOLID, KISS, DRY с фокусом на простой, расширяемый код. Реализуется только необходимая функциональность.

## Команды сборки

### Сборка приложения
```bash
./gradlew build
```

### Запуск Desktop (JVM) приложения
```bash
./gradlew :targets:desktop:run
```

### Запуск CLI приложения
```bash
./gradlew :targets:cli:run
```

## Обзор архитектуры

### Структура модулей

Проект организован в четыре основные категории:

#### 1. `tools/` - Переиспользуемые инструменты (без UI)
Бэкенд-компоненты и утилиты, обеспечивающие базовую функциональность без пользовательского интерфейса.

**Структура:**
- Один модуль: `tools/<name>/` (когда не нужны builders/factories или только реализация)
- Разделение API/Impl: `tools/<name>/api/` + `tools/<name>/impl/` (когда публичные интерфейсы и внутренние реализации разделены)
- Извлечение моделей: `tools/<category>/<component>/model/` (общие модели для избежания зависимости от репозиториев)

**Примеры:**
- `tools/llm/completions/` - LLM completion API клиент
- `tools/llm/config/model/` - Модели конфигурации LLM
- `tools/rag/core/` - RAG базовые модели и типы
- `tools/rag/embedder/` - Генерация эмбеддингов
- `tools/shared/mcp/model/` - MCP общие модели

#### 2. `core/` - Базовая инфраструктура
Фундаментальные модули, предоставляющие необходимые утилиты и инфраструктуру.

**Структура:**
- Один модуль: `core/<name>/` (когда только реализация, например, NavigationBus, Result, SafeCall)
- Разделение API/Impl: `core/<name>/api/` + `core/<name>/impl/` (когда нужна абстракция, например, Network, Dispatchers)

**Ключевые модули:**
- `core/network/api` + `core/network/impl` - Абстракция Ktor HttpClient
- `core/ui/mvi/` - MVI фреймворк (State, Intent, Effect, ViewModel)
- `core/ui/navigation/` - Контракты навигации (NavigationIntent)
- `core/ui/navigationbus/` - Реализация шины навигации
- `core/errors/` - Унифицированные типы ошибок (AppError)
- `core/result/` - Обёртка результата (AppResult)
- `core/safecall/` - Утилиты безопасного вызова с маппингом ошибок
- `core/dispatchers/` - Абстракция корутинных диспетчеров
- `core/logger/` - Абстракция логирования
- `core/utils/` - Общие утилиты

**Устаревшие:**
- `core/foundation/` - Мигрируется в специализированные модули

#### 3. `features/` - UI фичи
Модульные фичи с компонентами пользовательского интерфейса.

**Структура (всегда два модуля):**
```
features/<name>/
├── navigation/              # Контракты навигации
│   └── src/commonMain/kotlin/
│       └── <Name>NavigationIntent.kt
└── impl/                    # Реализация фичи
    ├── presentation/        # Слой UI (Compose экраны, ViewModels)
    ├── domain/              # Бизнес-логика (use cases, интерфейсы репозиториев)
    ├── data/                # Слой данных (реализации репозиториев)
    └── di/                  # Koin модули
```

**Правила:**
- Фичи имеют UI - если UI не нужен, создайте инструмент вместо этого
- Модуль навигации содержит только контракты `NavigationIntent`
- Модуль Impl содержит слои presentation/domain/data + реализацию `NavigationHandler`
- Фичи НЕ имеют отдельных API модулей (только контракты навигации)

**Текущие фичи:**
- `features/chat/navigation` - Контракты навигации чата (в разработке)
- `features/chat/impl` - Реализация чата (в процессе)

**Запланировано:**
- `features/mcp/` - Фича управления MCP
- `features/rag/` - Фича управления RAG

#### 4. `targets/` - Платформенные приложения
Конкретные приложения для специфических платформ.

**Структура:**
```
targets/
├── cli/         # CLI приложение (например, генерация биндингов)
├── desktop/     # Desktop JVM приложение
└── shared/      # Мультиплатформенное приложение (Android, iOS, Desktop)
```

**Статус:** Приложения пока не реализованы, структура подготовлена для миграции.

### Правила организации модулей

#### Когда использовать разделение API/Impl:
- **API модуль:** Публичные интерфейсы, контракты, модели, от которых зависят другие модули
- **Impl модуль:** Внутренние реализации, платформо-специфичный код, DI модули

#### Когда использовать один модуль:
- Модули только с реализацией (например, NavigationBus, Result, SafeCall)
- Не нужны builders, factories или сложные абстракции
- Модуль простой и не требует внутренней инкапсуляции

#### Извлечение моделей:
Извлекайте модели в отдельные модули, когда:
- Модели должны быть разделены между несколькими модулями
- Нужно избежать транзитивных зависимостей от реализаций репозиториев/сервисов

**Пример:** `tools/llm/config/model` - позволяет использовать модели конфигурации без зависимости от полной реализации хранилища конфигураций.

### Соглашения об именовании

#### Фичи:
```
features/<name>/navigation/     # Контракты навигации
features/<name>/impl/           # Реализация фичи
```

#### Инструменты:
```
tools/<name>/                       # Один модуль
tools/<name>/api/                   # Публичные интерфейсы
tools/<name>/impl/                  # Реализация
tools/<category>/<component>/model/ # Извлечённые модели
```

#### Ядро:
```
core/<name>/                    # Один модуль
core/<name>/api/                # Публичные интерфейсы
core/<name>/impl/               # Реализация
```

**Именование пакетов:**
- Фичи: `ru.izhxx.aichallenge.features.<name>.<layer>`
- Инструменты: `ru.izhxx.aichallenge.tools.<category>.<component>`
- Ядро: `ru.izhxx.aichallenge.core.<module>`

## Ключевые архитектурные компоненты

### Паттерн MVI (Model-View-Intent)

Проект использует MVI как основной архитектурный паттерн для UI. Расположен в `core/ui/mvi/`:

**Ключевые компоненты:**
- `MviIntent` - Действия/события пользователя (sealed интерфейсы)
- `MviState` - Неизменяемое состояние UI (data классы)
- `MviEffect` - Одноразовые побочные эффекты (навигация, тосты, диалоги)
- `MviViewModel` - Оркестрирует MVI конвейер

**Контракт:**
```kotlin
interface MviViewModel<S : MviState, E : MviEffect, I : MviIntent> {
    val state: StateFlow<S>     // Долгоживущее состояние UI
    val effects: Flow<E>        // Одноразовые побочные эффекты
    fun accept(intent: I)       // Точка входа для интентов
}
```

**Поток данных:**
```
Действие пользователя → Intent → Executor (async) → Result → Reduce → Новое состояние → UI
                                                     ↓
                                                 Effect → UI (одноразовый)
```

**Использование в Compose:**
```kotlin
@Composable
fun Screen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyEffect.ShowMessage -> /* обработать */
                is MyEffect.Navigate -> /* обработать */
            }
        }
    }

    // Отрисовка UI на основе состояния
    Button(onClick = { viewModel.accept(MyIntent.ButtonClicked) })
}
```

### Обработка ошибок

Унифицированная система обработки ошибок с типобезопасным распространением ошибок.

#### AppError - Унифицированные типы ошибок

Расположен в `core/errors/`, предоставляет комплексную таксономию ошибок:

**Категории:**
- **Сетевые ошибки:** `NetworkUnavailable`, `NetworkTimeout`, `NetworkSecurity`, `HttpError<M>`
- **Ошибки аутентификации:** `TokenExpired`, `InvalidCredentials`, `Unauthorized`, `ForbiddenPermissions`
- **Ошибки файловой системы:** `FileNotFound`, `Storage`
- **Ошибки данных:** `SerializationError`, `ValidationError`
- **Ограничение частоты:** `RateLimitError`
- **Разрешения:** `PermissionError`
- **Доменные ошибки:** `DomainError` (ошибки бизнес-логики)
- **Неизвестные ошибки:** `UnknownError` (запасной вариант)

**Свойства:**
```kotlin
sealed class AppError(
    val severity: ErrorSeverity,        // Critical, Error, Warning, Info
    val retry: ErrorRetry,              // Allowed, Forbidden, Unknown
    val cause: Throwable? = null,       // Исходное исключение
    val rawMessage: String? = null,     // Сообщение об ошибке
    val metadata: Map<MetadataKey, String> = emptyMap()  // Дополнительный контекст
)
```

#### AppResult - Обёртка результата

Расположен в `core/result/`, типобезопасный контейнер результата:

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()

    // Функциональные операции
    fun onSuccess(block: (T) -> Unit): AppResult<T>
    fun onFailure(block: (AppError) -> Unit): AppResult<T>
    fun getSuccessOrNull(): T?
}
```

**Использование:**
```kotlin
suspend fun fetchData(): AppResult<Data> = suspendedSafeCall {
    val response = httpClient.get("/data")
    response.body<Data>()
}

// В потребителе
fetchData()
    .onSuccess { data -> /* обработать данные */ }
    .onFailure { error -> /* обработать ошибку */ }
```

#### SafeCall - Конвертация исключений в AppResult

Расположен в `core/safecall/`, обеспечивает безопасное выполнение с автоматическим маппингом ошибок:

**Правила:**
- **Используйте SafeCall для:**
  - Сетевых вызовов
  - Операций с файловой системой
  - Любых операций, которые могут выбросить исключения
- **НЕ используйте SafeCall для:**
  - Запросов к базе данных Room (Room обрабатывает ошибки внутренне)
  - Чистой бизнес-логики (используйте доменные ошибки вместо этого)

**API:**
```kotlin
// Синхронный
fun <T> safeCall(
    throwableMapper: (Throwable) -> AppError = ::defaultUnknownError,
    block: () -> T
): AppResult<T>

// Асинхронный
suspend fun <T> suspendedSafeCall(
    throwableMapper: (Throwable) -> AppError = ::defaultUnknownError,
    block: suspend () -> T
): AppResult<T>
```

**Контракт маппинга ошибок:**
- Вся конвертация `Throwable` → `AppError` происходит **только в слое данных**
- Каждый источник данных (сеть, файловая система) предоставляет кастомный маппер
- Маппер должен анализировать тип исключения и возвращать конкретный подтип `AppError`

**Пример:**
```kotlin
// В реализации репозитория (слой данных)
class MyRepositoryImpl(
    private val httpClient: HttpClient
) : MyRepository {

    override suspend fun getData(): AppResult<Data> = suspendedSafeCall(
        throwableMapper = ::mapHttpException
    ) {
        httpClient.get("/data").body<Data>()
    }

    private fun mapHttpException(t: Throwable): AppError = when (t) {
        is ClientRequestException -> when (t.response.status.value) {
            401 -> AppError.Unauthorized
            403 -> AppError.ForbiddenPermissions
            404 -> AppError.HttpError(code = 404, rawMessage = "Ресурс не найден")
            else -> AppError.HttpError(code = t.response.status.value, cause = t)
        }
        is ServerResponseException -> AppError.HttpError(
            code = t.response.status.value,
            cause = t,
            rawMessage = "Ошибка сервера"
        )
        is HttpRequestTimeoutException -> AppError.NetworkTimeout
        is IOException -> AppError.NetworkUnavailable
        else -> AppError.UnknownError(
            severity = ErrorSeverity.Error,
            retry = ErrorRetry.Unknown,
            cause = t
        )
    }
}
```

### Навигация

Децентрализованная система навигации на основе NavigationBus с типобезопасными интентами.

#### Компоненты архитектуры:

1. **NavigationIntent** (`core/ui/navigation/`)
   - Интерфейс-маркер для контрактов навигации
   - Каждая фича определяет свою собственную sealed иерархию

2. **NavigationBus** (`core/ui/navigationbus/`)
   - Центральная шина событий для событий навигации
   - Регистрирует NavigationHandlers
   - Маршрутизирует интенты к соответствующим обработчикам

3. **NavigationHandler** (в `features/<name>/impl`)
   - Обрабатывает интенты навигации для конкретной фичи
   - Взаимодействует с Androidx Navigation Controller

**Контракт навигации фичи (в `features/<name>/navigation`):**
```kotlin
// Контракт навигации - что могут использовать другие фичи
interface ChatNavigationIntent : NavigationIntent

data class OpenChatIntent(val conversationId: String?) : ChatNavigationIntent
data object OpenNewChatIntent : ChatNavigationIntent
```

**Обработчик навигации (в `features/<name>/impl/presentation/navigation`):**
```kotlin
internal class ChatNavigationHandler(
    private val navController: NavController
) : NavigationHandler<ChatNavigationIntent> {

    override fun handle(intent: ChatNavigationIntent): Boolean {
        when (intent) {
            is OpenChatIntent -> navController.navigate("chat/${intent.conversationId}")
            is OpenNewChatIntent -> navController.navigate("chat/new")
        }
        return true
    }
}
```

**Использование в приложении (target):**
```kotlin
// В настройке Application/Activity
val navigationBus: NavigationBus = get()

// Регистрация обработчиков
val chatHandler = ChatNavigationHandler(navController)
navigationBus.register(chatHandler)

navigationBus.markReady() // Обработка ожидающих интентов

// Навигация из любой фичи
class SomeViewModel(private val navigationBus: NavigationBus) {
    fun openChat() {
        navigationBus.send(OpenChatIntent(conversationId = "123"))
    }
}
```

**Преимущества:**
- Фичи разделены - зависят только от контрактов навигации
- Легко добавлять/удалять фичи
- Типобезопасная навигация
- Поддержка запасных вариантов для отсутствующих фич

### Внедрение зависимостей (Koin)

Все фичи и модули используют Koin для внедрения зависимостей.

#### Правила:

1. **Расположение DI модулей:**
   - Модули создаются **только в директориях `impl/`**
   - Всегда `public` видимость
   - Всегда определяются как `val` переменная
   - Не требуется явное объявление типа

2. **Структура модуля:**
```kotlin
// В features/<name>/impl/di/<Name>Module.kt
val chatModule = module {
    // ViewModels
    viewModel { ChatViewModel(get(), get()) }

    // Use cases
    factory<SendMessageUseCase> { SendMessageUseCaseImpl(get()) }

    // Repositories
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }

    // Navigation handler
    factory { (navController: NavController) ->
        ChatNavigationHandler(navController)
    }
}
```

3. **Платформо-специфичные зависимости:**

**Вариант A: expect/actual внутренний модуль**
```kotlin
// commonMain
internal expect class PlatformHttpEngine()

// androidMain
internal actual class PlatformHttpEngine actual constructor() {
    fun create(): HttpClientEngine = Android.create()
}

// В Koin модуле
val networkModule = module {
    single { PlatformHttpEngine().create() }
}
```

**Вариант B: expect/actual функция-фабрика**
```kotlin
// commonMain
expect fun createHttpEngine(): HttpClientEngine

// androidMain
actual fun createHttpEngine(): HttpClientEngine = Android.create()

// В Koin модуле
val networkModule = module {
    single { createHttpEngine() }
}
```

4. **Сборка модулей в Targets:**
```kotlin
// В классе Application
fun Application.setupKoin() {
    startKoin {
        androidContext(this@setupKoin)
        modules(
            // Core
            networkModule,
            dispatchersModule,

            // Tools
            llmModule,
            ragModule,

            // Features
            chatModule,

            // App-specific
            appModule
        )
    }
}
```

### Сеть (HttpClient)

Абстракция над Ktor HttpClient для платформо-независимых HTTP операций.

Расположен в `core/network/api` + `core/network/impl`.

**Интерфейс (`core/network/api`):**
```kotlin
interface HttpClientCreator {
    fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
}
```

**Использование:**
```kotlin
class ApiService(private val httpClientCreator: HttpClientCreator) {
    private val client = httpClientCreator.buildHttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            })
        }

        install(Logging) {
            level = LogLevel.BODY
        }

        defaultRequest {
            url("https://api.example.com/")
        }
    }

    suspend fun fetchData(): String = client.get("endpoint").bodyAsText()
}
```

## Слоистая архитектура

### Структура фич

Фичи следуют Clean Architecture с паттерном MVI:

```
features/<name>/
├── navigation/                     # Контракты навигации (публичные)
│   └── src/commonMain/kotlin/
│       └── <Name>NavigationIntent.kt
│
└── impl/                           # Реализация (внутренняя)
    ├── presentation/               # Слой UI
    │   ├── <Name>Screen.kt        # Compose UI
    │   ├── <Name>ViewModel.kt     # Реализация MviViewModel
    │   ├── navigation/
    │   │   └── <Name>NavigationHandler.kt
    │   ├── model/
    │   │   ├── <Name>Intent.kt    # MVI Intents
    │   │   ├── <Name>State.kt     # MVI State
    │   │   └── <Name>Effect.kt    # MVI Effects
    │   ├── components/             # Переиспользуемые UI компоненты
    │   └── mapper/                 # Маппинг Domain ↔ UI
    │
    ├── domain/                     # Слой бизнес-логики
    │   ├── model/                  # Доменные модели
    │   ├── repository/             # Интерфейсы репозиториев
    │   └── usecase/
    │       ├── <Action><Entity>UseCase.kt
    │       └── <Action><Entity>UseCaseImpl.kt
    │
    ├── data/                       # Слой данных
    │   ├── repository/
    │   │   └── <Name>RepositoryImpl.kt
    │   ├── datasource/            # Источники данных (удалённые, локальные)
    │   └── mapper/                # Маппинг DTO ↔ Domain
    │
    └── di/
        └── <Name>Module.kt        # Koin DI модуль
```

**Правила зависимостей:**
- Presentation → Domain ✓
- Presentation → Data ✗ (использовать доменные интерфейсы)
- Domain → Data ✗ (домен определяет интерфейсы)
- Data → Domain ✓ (данные реализуют доменные интерфейсы)

### Структура инструментов

Инструменты - это бэкенд компоненты без UI:

```
tools/<category>/<component>/
├── api/                            # Публичные интерфейсы (опционально)
│   ├── model/                     # Публичные модели
│   ├── repository/                # Интерфейсы репозиториев
│   └── usecase/                   # Интерфейсы UseCase
│
└── impl/                           # Реализация
    ├── data/
    │   └── repository/
    │       └── <Name>RepositoryImpl.kt
    ├── domain/
    │   └── usecase/
    │       └── <Name>UseCaseImpl.kt
    └── di/
        └── <Name>Module.kt
```

**Или один модуль, когда разделение API не нужно:**
```
tools/<category>/<component>/
├── model/                          # Доменные модели
├── repository/                     # Реализации
└── di/
    └── <Name>Module.kt
```

### Модули ядра

Модули ядра предоставляют фундаментальные утилиты:

**С разделением API/Impl (нужна абстракция):**
```
core/<name>/
├── api/                            # Публичные интерфейсы
│   └── src/commonMain/kotlin/
│       └── <Component>.kt
└── impl/                           # Платформо-специфичная реализация
    ├── src/commonMain/kotlin/
    ├── src/androidMain/kotlin/    # Android реализация
    ├── src/iosMain/kotlin/        # iOS реализация
    └── src/jvmMain/kotlin/        # JVM реализация
```

**Один модуль (только реализация):**
```
core/<name>/
└── src/
    ├── commonMain/kotlin/         # Общая реализация
    ├── androidMain/kotlin/        # Android-специфичная
    ├── iosMain/kotlin/           # iOS-специфичная
    └── jvmMain/kotlin/           # JVM-специфичная
```

## Работа с Gradle

### Convention плагины

Расположены в `build-logic/logic/src/main/kotlin/`.

**Текущий плагин: `shared.library`**
- Используется для всех Kotlin Multiplatform библиотечных модулей
- Настраивает Android, iOS и JVM таргеты
- Применяет конфигурацию линтера

**Использование:**
```kotlin
// В build.gradle.kts модуля
plugins {
    id("shared.library")
}

android {
    config("module.package.name")
}

kotlin {
    commonDependencies {
        implementation(projects.core.result)
        implementation(libs.kotlinx.coroutines.core)
    }
}
```

**Устаревшие плагины:**
- `jvm.library` - старый JVM-only библиотечный плагин
- `feature.library` - предназначался для фич, никогда не использовался

### Обнаружение модулей

Модули автоматически обнаруживаются из корневых директорий, определённых в `settings.gradle.kts`:
- `core/`
- `tools/`
- `features/`
- `targets/`

Новые модули с файлами `build.gradle.kts` автоматически включаются в сборку.

### Кэш конфигурации

Проект использует кэш конфигурации Gradle (`org.gradle.configuration-cache=true`).

### Настройки JVM

- Таргет: Java 21
- Максимальная куча: 8GB (`org.gradle.jvmargs=-Xmx8G`)
- Kotlin daemon: 8GB (`kotlin.daemon.jvmargs=-Xmx8G`)

## Ключевые технологии

- **Kotlin**: 2.2.21
- **Compose Multiplatform**: 1.9.3
- **Ktor Client**: 3.3.3 (сеть)
- **Ktor Server**: 3.3.3 (бэкенд, будущее использование)
- **Koin**: 4.1.1 (внедрение зависимостей)
- **Kotlinx Coroutines**: 1.10.2
- **Kotlinx Serialization**: 1.9.0
- **Androidx Navigation Compose**: Для навигации в Compose Multiplatform

## Соглашения проекта

### Соглашения об именовании

**Use Cases:**
```kotlin
// Интерфейс
interface SendMessageUseCase
// Реализация
class SendMessageUseCaseImpl(/*...*/) : SendMessageUseCase
```

**ViewModels:**
```kotlin
class ChatViewModel : MviViewModel<ChatState, ChatEffect, ChatIntent>
```

**MVI компоненты:**
```kotlin
sealed interface ChatIntent : MviIntent
data class ChatState(/*...*/) : MviState
sealed interface ChatEffect : MviEffect
```

**Модели:**
- DTOs: суффикс `DTO` (например, `ChatMessageDTO`)
- Entities: суффикс `Entity` (например, `MessageEntity`)
- Доменные модели: без суффикса (например, `Message`, `User`)

### Управление состоянием

Всё состояние UI управляется через MVI:
- Состояние **неизменяемо** (data классы, sealed иерархии)
- Обновления состояния **однонаправленные** (Intent → State)
- Побочные эффекты отделены от состояния (Effect flow)
- ViewModels предоставляют `StateFlow<State>` и `Flow<Effect>`

### Платформо-специфичный код

Используйте `expect`/`actual` объявления для платформенных абстракций:

```kotlin
// commonMain
expect fun getCurrentTimestamp(): Long

// androidMain
actual fun getCurrentTimestamp(): Long = System.currentTimeMillis()

// iosMain
actual fun getCurrentTimestamp(): Long =
    NSDate().timeIntervalSince1970.toLong() * 1000
```

**Структура source set:**
- `src/commonMain/kotlin/` - Разделяется между всеми платформами
- `src/androidMain/kotlin/` - Android-специфичный
- `src/iosMain/kotlin/` - iOS-специфичный
- `src/jvmMain/kotlin/` - JVM/Desktop-специфичный

## Статус миграции

Этот проект проходит миграцию со старой архитектуры на новую модульную структуру.

**Активные (новая структура):**
- `tools/` - новые модульные инструменты
- `core/` (кроме `core/foundation/`)
- `features/chat/` (в процессе)
- `targets/` (структура подготовлена)
- `build-logic/` - convention плагины

**Устаревшие (старая структура):**
- `composeApp/` - старое приложение, мигрируется
- `shared/` - старая общая логика, мигрируется
- `instances/` - старые MCP серверы, мигрируются
- `rag/` - старая RAG система, мигрируется в `tools/rag/`
- `core/foundation/` - разделяется на специализированные модули

**Не ссылайтесь и не изменяйте устаревшие модули.**
