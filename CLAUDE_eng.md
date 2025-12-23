# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform** project implementing an AI Challenge application with support for Android,
iOS, Desktop (JVM), and CLI platforms.
The codebase uses modern Kotlin practices with Compose Multiplatform for UI,
Ktor for networking, and follows Clean Architecture principles with MVI pattern.

**Core Principles:** SOLID, KISS, DRY with focus on simple, extensible code. Only necessary functionality is implemented.

## Build Commands

### Build the Application
```bash
./gradlew build
```

### Run Desktop (JVM) Application
```bash
./gradlew :targets:desktop:run
```

### Run CLI Application
```bash
./gradlew :targets:cli:run
```

## Architecture Overview

### Module Structure

The project is organized into four main categories:

#### 1. `tools/` - Reusable Instruments (No UI)
Backend components and utilities that provide core functionality without user interface.

**Structure:**
- Single module: `tools/<name>/` (when no builders/factories needed, or implementation-only)
- API/Impl split: `tools/<name>/api/` + `tools/<name>/impl/` (when public interfaces and internal implementations are separated)
- Model extraction: `tools/<category>/<component>/model/` (shared models to avoid dependency on repositories)

**Examples:**
- `tools/llm/completions/` - LLM completion API client
- `tools/llm/config/model/` - LLM configuration models
- `tools/rag/core/` - RAG core models and types
- `tools/rag/embedder/` - Embedding generation
- `tools/shared/mcp/model/` - MCP shared models

#### 2. `core/` - Core Infrastructure
Foundation modules providing essential utilities and infrastructure.

**Structure:**
- Single module: `core/<name>/` (when implementation-only, e.g., NavigationBus, Result, SafeCall)
- API/Impl split: `core/<name>/api/` + `core/<name>/impl/` (when abstraction needed, e.g., Network, Dispatchers)

**Key Modules:**
- `core/network/api` + `core/network/impl` - Ktor HttpClient abstraction
- `core/ui/mvi/` - MVI framework (State, Intent, Effect, ViewModel)
- `core/ui/navigation/` - Navigation contracts (NavigationIntent)
- `core/ui/navigationbus/` - Navigation bus implementation
- `core/errors/` - Unified error types (AppError)
- `core/result/` - Result wrapper (AppResult)
- `core/safecall/` - Safe call utilities with error mapping
- `core/dispatchers/` - Coroutine dispatchers abstraction
- `core/logger/` - Logging abstraction
- `core/utils/` - Common utilities

**Deprecated:**
- `core/foundation/` - Being migrated to specialized modules

#### 3. `features/` - UI Features
Modular features with user interface components.

**Structure (always two modules):**
```
features/<name>/
├── navigation/              # Navigation contracts
│   └── src/commonMain/kotlin/
│       └── <Name>NavigationIntent.kt
└── impl/                    # Feature implementation
    ├── presentation/        # UI layer (Compose screens, ViewModels)
    ├── domain/              # Business logic (use cases, repository interfaces)
    ├── data/                # Data layer (repository implementations)
    └── di/                  # Koin modules
```

**Rules:**
- Features have UI - if no UI needed, create a tool instead
- Navigation module contains only `NavigationIntent` contracts
- Impl module contains presentation/domain/data layers + `NavigationHandler` implementation
- Features do NOT have separate API modules (only navigation contracts)

**Current Features:**
- `features/chat/navigation` - Chat navigation contracts (WIP)
- `features/chat/impl` - Chat implementation (in progress)

**Planned:**
- `features/mcp/` - MCP management feature
- `features/rag/` - RAG management feature

#### 4. `targets/` - Platform Applications
Concrete applications for specific platforms.

**Structure:**
```
targets/
├── cli/         # CLI application (e.g., binding generation)
├── desktop/     # Desktop JVM application
└── shared/      # Multi-platform app (Android, iOS, Desktop)
```

**Status:** No applications implemented yet, structure prepared for migration.

### Module Organization Rules

#### When to use API/Impl split:
- **API module:** Public interfaces, contracts, models that other modules depend on
- **Impl module:** Internal implementations, platform-specific code, DI modules

#### When to use single module:
- Implementation-only modules (e.g., NavigationBus, Result, SafeCall)
- No builders, factories, or complex abstractions needed
- Module is simple and doesn't need internal encapsulation

#### Model extraction:
Extract models into separate modules when:
- Models need to be shared between multiple modules
- Want to avoid transitive dependencies on repository/service implementations

**Example:** `tools/llm/config/model` - allows using config models without depending on full config storage implementation.

### Naming Conventions

#### Features:
```
features/<name>/navigation/     # Navigation contracts
features/<name>/impl/           # Feature implementation
```

#### Tools:
```
tools/<name>/                       # Single module
tools/<name>/api/                   # Public interfaces
tools/<name>/impl/                  # Implementation
tools/<category>/<component>/model/ # Extracted models
```

#### Core:
```
core/<name>/                    # Single module
core/<name>/api/                # Public interfaces
core/<name>/impl/               # Implementation
```

**Package naming:**
- Features: `ru.izhxx.aichallenge.features.<name>.<layer>`
- Tools: `ru.izhxx.aichallenge.tools.<category>.<component>`
- Core: `ru.izhxx.aichallenge.core.<module>`

## Key Architectural Components

### MVI Pattern (Model-View-Intent)

The project uses MVI as the primary architectural pattern for UI. Located in `core/ui/mvi/`:

**Key Components:**
- `MviIntent` - User actions/events (sealed interfaces)
- `MviState` - Immutable UI state (data classes)
- `MviEffect` - One-time side effects (navigation, toasts, dialogs)
- `MviViewModel` - Orchestrates the MVI pipeline

**Contract:**
```kotlin
interface MviViewModel<S : MviState, E : MviEffect, I : MviIntent> {
    val state: StateFlow<S>     // Long-lived UI state
    val effects: Flow<E>        // One-time side effects
    fun accept(intent: I)       // Entry point for intents
}
```

**Data Flow:**
```
User Action → Intent → Executor (async) → Result → Reduce → New State → UI
                                        ↓
                                    Effect → UI (one-time)
```

**Usage in Compose:**
```kotlin
@Composable
fun Screen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyEffect.ShowMessage -> /* handle */
                is MyEffect.Navigate -> /* handle */
            }
        }
    }

    // UI rendering based on state
    Button(onClick = { viewModel.accept(MyIntent.ButtonClicked) })
}
```

### Error Handling

Unified error handling system with type-safe error propagation.

#### AppError - Unified Error Types

Located in `core/errors/`, provides comprehensive error taxonomy:

**Categories:**
- **Network errors:** `NetworkUnavailable`, `NetworkTimeout`, `NetworkSecurity`, `HttpError<M>`
- **Auth errors:** `TokenExpired`, `InvalidCredentials`, `Unauthorized`, `ForbiddenPermissions`
- **File system errors:** `FileNotFound`, `Storage`
- **Data errors:** `SerializationError`, `ValidationError`
- **Rate limiting:** `RateLimitError`
- **Permissions:** `PermissionError`
- **Domain errors:** `DomainError` (business logic errors)
- **Unknown errors:** `UnknownError` (fallback)

**Properties:**
```kotlin
sealed class AppError(
    val severity: ErrorSeverity,        // Critical, Error, Warning, Info
    val retry: ErrorRetry,              // Allowed, Forbidden, Unknown
    val cause: Throwable? = null,       // Original exception
    val rawMessage: String? = null,     // Error message
    val metadata: Map<MetadataKey, String> = emptyMap()  // Additional context
)
```

#### AppResult - Result Wrapper

Located in `core/result/`, type-safe result container:

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()

    // Functional operations
    fun onSuccess(block: (T) -> Unit): AppResult<T>
    fun onFailure(block: (AppError) -> Unit): AppResult<T>
    fun getSuccessOrNull(): T?
}
```

**Usage:**
```kotlin
suspend fun fetchData(): AppResult<Data> = suspendedSafeCall {
    val response = httpClient.get("/data")
    response.body<Data>()
}

// In consumer
fetchData()
    .onSuccess { data -> /* process data */ }
    .onFailure { error -> /* handle error */ }
```

#### SafeCall - Exception to AppResult Conversion

Located in `core/safecall/`, provides safe execution with automatic error mapping:

**Rules:**
- **Use SafeCall for:**
  - Network calls
  - File system operations
  - Any operation that may throw exceptions
- **Do NOT use SafeCall for:**
  - Room database queries (Room handles errors internally)
  - Pure business logic (use domain errors instead)

**API:**
```kotlin
// Synchronous
fun <T> safeCall(
    throwableMapper: (Throwable) -> AppError = ::defaultUnknownError,
    block: () -> T
): AppResult<T>

// Asynchronous
suspend fun <T> suspendedSafeCall(
    throwableMapper: (Throwable) -> AppError = ::defaultUnknownError,
    block: suspend () -> T
): AppResult<T>
```

**Error Mapping Contract:**
- All `Throwable` → `AppError` conversion happens in **data layer only**
- Each data source (network, file system) provides custom mapper
- Mapper should analyze exception type and return specific `AppError` subtype

**Example:**
```kotlin
// In repository implementation (data layer)
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
            404 -> AppError.HttpError(code = 404, rawMessage = "Resource not found")
            else -> AppError.HttpError(code = t.response.status.value, cause = t)
        }
        is ServerResponseException -> AppError.HttpError(
            code = t.response.status.value,
            cause = t,
            rawMessage = "Server error"
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

### Navigation

Decentralized navigation system based on NavigationBus with type-safe intents.

#### Architecture Components:

1. **NavigationIntent** (`core/ui/navigation/`)
   - Marker interface for navigation contracts
   - Each feature defines its own sealed hierarchy

2. **NavigationBus** (`core/ui/navigationbus/`)
   - Central event bus for navigation events
   - Registers NavigationHandlers
   - Routes intents to appropriate handlers

3. **NavigationHandler** (in `features/<name>/impl`)
   - Handles navigation intents for specific feature
   - Interacts with Androidx Navigation Controller

**Feature Navigation Contract (in `features/<name>/navigation`):**
```kotlin
// Navigation contract - what other features can use
interface ChatNavigationIntent : NavigationIntent

data class OpenChatIntent(val conversationId: String?) : ChatNavigationIntent
data object OpenNewChatIntent : ChatNavigationIntent
```

**Navigation Handler (in `features/<name>/impl/presentation/navigation`):**
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

**Usage in Application (target):**
```kotlin
// In Application/Activity setup
val navigationBus: NavigationBus = get()

// Register handlers
val chatHandler = ChatNavigationHandler(navController)
navigationBus.register(chatHandler)

navigationBus.markReady() // Process pending intents

// Navigate from any feature
class SomeViewModel(private val navigationBus: NavigationBus) {
    fun openChat() {
        navigationBus.send(OpenChatIntent(conversationId = "123"))
    }
}
```

**Benefits:**
- Features are decoupled - only depend on navigation contracts
- Easy to add/remove features
- Type-safe navigation
- Fallback support for missing features

### Dependency Injection (Koin)

All features and modules use Koin for dependency injection.

#### Rules:

1. **DI Modules Location:**
   - Modules created **only in `impl/` directories**
   - Always `public` visibility
   - Always defined as `val` variable
   - No explicit type declaration needed

2. **Module Structure:**
```kotlin
// In features/<name>/impl/di/<Name>Module.kt
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

3. **Platform-Specific Dependencies:**

**Option A: expect/actual internal module**
```kotlin
// commonMain
internal expect class PlatformHttpEngine()

// androidMain
internal actual class PlatformHttpEngine actual constructor() {
    fun create(): HttpClientEngine = Android.create()
}

// In Koin module
val networkModule = module {
    single { PlatformHttpEngine().create() }
}
```

**Option B: expect/actual factory function**
```kotlin
// commonMain
expect fun createHttpEngine(): HttpClientEngine

// androidMain
actual fun createHttpEngine(): HttpClientEngine = Android.create()

// In Koin module
val networkModule = module {
    single { createHttpEngine() }
}
```

4. **Module Assembly in Targets:**
```kotlin
// In Application class
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

### Network (HttpClient)

Abstraction over Ktor HttpClient for platform-agnostic HTTP operations.

Located in `core/network/api` + `core/network/impl`.

**Interface (`core/network/api`):**
```kotlin
interface HttpClientCreator {
    fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
}
```

**Usage:**
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

## Layer Architecture

### Features Structure

Features follow Clean Architecture with MVI pattern:

```
features/<name>/
├── navigation/                     # Navigation contracts (public)
│   └── src/commonMain/kotlin/
│       └── <Name>NavigationIntent.kt
│
└── impl/                           # Implementation (internal)
    ├── presentation/               # UI Layer
    │   ├── <Name>Screen.kt        # Compose UI
    │   ├── <Name>ViewModel.kt     # MviViewModel implementation
    │   ├── navigation/
    │   │   └── <Name>NavigationHandler.kt
    │   ├── model/
    │   │   ├── <Name>Intent.kt    # MVI Intents
    │   │   ├── <Name>State.kt     # MVI State
    │   │   └── <Name>Effect.kt    # MVI Effects
    │   ├── components/             # Reusable UI components
    │   └── mapper/                 # Domain ↔ UI mapping
    │
    ├── domain/                     # Business Logic Layer
    │   ├── model/                  # Domain models
    │   ├── repository/             # Repository interfaces
    │   └── usecase/
    │       ├── <Action><Entity>UseCase.kt
    │       └── <Action><Entity>UseCaseImpl.kt
    │
    ├── data/                       # Data Layer
    │   ├── repository/
    │   │   └── <Name>RepositoryImpl.kt
    │   ├── datasource/            # Data sources (remote, local)
    │   └── mapper/                # DTO ↔ Domain mapping
    │
    └── di/
        └── <Name>Module.kt        # Koin DI module
```

**Dependency Rules:**
- Presentation → Domain ✓
- Presentation → Data ✗ (use domain interfaces)
- Domain → Data ✗ (domain defines interfaces)
- Data → Domain ✓ (data implements domain interfaces)

### Tools Structure

Tools are backend components without UI:

```
tools/<category>/<component>/
├── api/                            # Public interfaces (optional)
│   ├── model/                     # Public models
│   ├── repository/                # Repository interfaces
│   └── usecase/                   # UseCase interfaces
│
└── impl/                           # Implementation
    ├── data/
    │   └── repository/
    │       └── <Name>RepositoryImpl.kt
    ├── domain/
    │   └── usecase/
    │       └── <Name>UseCaseImpl.kt
    └── di/
        └── <Name>Module.kt
```

**Or single module when no API split needed:**
```
tools/<category>/<component>/
├── model/                          # Domain models
├── repository/                     # Implementations
└── di/
    └── <Name>Module.kt
```

### Core Modules

Core modules provide foundation utilities:

**With API/Impl split (abstraction needed):**
```
core/<name>/
├── api/                            # Public interfaces
│   └── src/commonMain/kotlin/
│       └── <Component>.kt
└── impl/                           # Platform-specific implementation
    ├── src/commonMain/kotlin/
    ├── src/androidMain/kotlin/    # Android implementation
    ├── src/iosMain/kotlin/        # iOS implementation
    └── src/jvmMain/kotlin/        # JVM implementation
```

**Single module (implementation-only):**
```
core/<name>/
└── src/
    ├── commonMain/kotlin/         # Common implementation
    ├── androidMain/kotlin/        # Android-specific
    ├── iosMain/kotlin/           # iOS-specific
    └── jvmMain/kotlin/           # JVM-specific
```

## Working with Gradle

### Convention Plugins

Located in `build-logic/logic/src/main/kotlin/`.

**Current plugin: `shared.library`**
- Used for all Kotlin Multiplatform library modules
- Configures Android, iOS, and JVM targets
- Applies lint configuration

**Usage:**
```kotlin
// In module's build.gradle.kts
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

**Deprecated plugins:**
- `jvm.library` - old JVM-only library plugin
- `feature.library` - was intended for features, never used

### Module Discovery

Modules are auto-discovered from root directories defined in `settings.gradle.kts`:
- `core/`
- `tools/`
- `features/`
- `targets/`

New modules with `build.gradle.kts` files are automatically included in the build.

### Configuration Cache

Project uses Gradle configuration cache (`org.gradle.configuration-cache=true`).

### JVM Settings

- Target: Java 21
- Max heap: 8GB (`org.gradle.jvmargs=-Xmx8G`)
- Kotlin daemon: 8GB (`kotlin.daemon.jvmargs=-Xmx8G`)

## Key Technologies

- **Kotlin**: 2.2.21
- **Compose Multiplatform**: 1.9.3
- **Ktor Client**: 3.3.3 (networking)
- **Ktor Server**: 3.3.3 (backend, future use)
- **Koin**: 4.1.1 (dependency injection)
- **Kotlinx Coroutines**: 1.10.2
- **Kotlinx Serialization**: 1.9.0
- **Androidx Navigation Compose**: For navigation in Compose Multiplatform

## Project Conventions

### Naming Conventions

**Use Cases:**
```kotlin
// Interface
interface SendMessageUseCase
// Implementation
class SendMessageUseCaseImpl(/*...*/) : SendMessageUseCase
```

**ViewModels:**
```kotlin
class ChatViewModel : MviViewModel<ChatState, ChatEffect, ChatIntent>
```

**MVI Components:**
```kotlin
sealed interface ChatIntent : MviIntent
data class ChatState(/*...*/) : MviState
sealed interface ChatEffect : MviEffect
```

**Models:**
- DTOs: suffix with `DTO` (e.g., `ChatMessageDTO`)
- Entities: suffix with `Entity` (e.g., `MessageEntity`)
- Domain models: no suffix (e.g., `Message`, `User`)

### State Management

All UI state is managed through MVI:
- State is **immutable** (data classes, sealed hierarchies)
- State updates are **unidirectional** (Intent → State)
- Side effects are separate from state (Effect flow)
- ViewModels expose `StateFlow<State>` and `Flow<Effect>`

### Platform-Specific Code

Use `expect`/`actual` declarations for platform abstractions:

```kotlin
// commonMain
expect fun getCurrentTimestamp(): Long

// androidMain
actual fun getCurrentTimestamp(): Long = System.currentTimeMillis()

// iosMain
actual fun getCurrentTimestamp(): Long =
    NSDate().timeIntervalSince1970.toLong() * 1000
```

**Source set structure:**
- `src/commonMain/kotlin/` - Shared across all platforms
- `src/androidMain/kotlin/` - Android-specific
- `src/iosMain/kotlin/` - iOS-specific
- `src/jvmMain/kotlin/` - JVM/Desktop-specific

## Migration Status

This project is undergoing migration from legacy architecture to new modular structure.

**Active (new structure):**
- `tools/` - new modular tools
- `core/` (except `core/foundation/`)
- `features/chat/` (in progress)
- `targets/` (structure prepared)
- `build-logic/` - convention plugins

**Deprecated (legacy structure):**
- `composeApp/` - legacy app, being migrated
- `shared/` - legacy shared logic, being migrated
- `instances/` - legacy MCP servers, being migrated
- `rag/` - legacy RAG system, being migrated to `tools/rag/`
- `core/foundation/` - being split into specialized modules

**Do not reference or modify deprecated modules.**
