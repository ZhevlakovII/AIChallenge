# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform** project implementing an AI Challenge application with support for Android, Desktop (JVM), and Server platforms. The codebase uses modern Kotlin practices with Compose Multiplatform for UI, Ktor for networking and server, and follows Clean Architecture principles with MVI pattern.

## Build Commands

### Build the Application
```bash
./gradlew build
```

### Run Android Application
```bash
./gradlew :composeApp:assembleDebug
```

### Run Desktop (JVM) Application
```bash
./gradlew :composeApp:run
```

### Run Server
```bash
./gradlew :server:run
```

## Architecture Overview

### Module Structure

The project uses automatic module discovery from these root directories:
- `app/` - Main Compose Multiplatform application
- `core/` - Core infrastructure and utilities
- `designsystem/` - UI design system components
- `features/` - Modular feature implementations
- `instruments/` - Tools that are used in the application
- `composeApp/` - Main Compose Multiplatform application (legacy, being migrated)
- `shared/` - Shared business logic across platforms (legacy, being migrated)
- `instances/` - Server instances (MCP servers) (legacy, being migrated)
- `rag/` - RAG (Retrieval-Augmented Generation) system (legacy, being migrated)
- `targets/` - Platform-based application/instruments implementations

### Core Modules

#### `core/network/`
A lightweight module for creating Ktor HttpClient. It is split into `api` and `impl` modules. Consumers use the `api` module.

#### `core/ui/`
- `core/ui/mvi/` - MVI (Model-View-Intent) framework
- `core/ui/navigation/` - Navigation infrastructure

#### `core/foundation/`
Foundation utilities and helpers used across the application.

### MVI Pattern (Model-View-Intent)

The project uses MVI as the primary architectural pattern for UI. Located in `core/ui/mvi/`:

**Key Components:**
- `MviIntent` - User actions/events (sealed interfaces)
- `MviState` - Immutable UI state (data classes)
- `MviResult` - Intermediate results from business logic
- `MviEffect` - One-time side effects (navigation, toasts)
- `MviViewModel` - Orchestrates the MVI pipeline
- `MviExecutor` - Handles async business logic
- `MviReducer` - Pure state transformation function

**Data Flow:**
```
User Action → Intent → Executor (async) → Result/Effect → Reducer → New State → UI
```

### Feature Structure

Features follow a consistent modular pattern with separation of concerns:

```
features/<feature-name>/
├── api/                    # Navigation interface contract
│   └── build.gradle.kts
└── impl/                   # Implementation
    ├── di/
    │   └── <Feature>Module.kt         # Koin DI module
    ├── data/
    │   └── repository/
    │       └── <FeatureRepository>Impl.kt       # Implementation    
    ├── domain/
    │   ├── repository/
    │   │   └── <FeatureRepository>.kt      # Interface if the feature works independently with the data layer
    │   ├── model/
    │   │   └── <FeatureModel>.kt      # Internal Domain models if need
    │   └── usecase/
    │       ├── <FeatureUseCase>.kt           # Interface
    │       └── <FeatureUseCase>Impl.kt       # Implementation
    ├── presentation/
    │   ├── <Feature>Screen.kt         # Compose UI
    │   ├── <Feature>ViewModel.kt      # MVI ViewModel
    │   ├── model/
    │   │   ├── <Feature>Event.kt      # Intents/Events
    │   │   ├── <Feature>State.kt      # UI State
    │   │   └── <Feature>Effect.kt     # Side effects
    │   ├── components/                # Reusable UI components
    │   └── mapper/                    # Domain ↔ UI mapping
    └── build.gradle.kts
```

**Example Features:**
- `features/chat/` - Chat interface with LLM (WIP)

### Instruments Structure

Instruments follow a consistent modular pattern with separation of concerns:

```
instrument/<instrument-name>/
├── api/                    # Public models, contracts (use-cases, repositories, etc)
│   ├── domain/
│   │   ├── repository/
│   │   │   └── <FeatureRepository>.kt      # Public interface
│   │   ├── model/
│   │   │   └── <FeatureModel>.kt      # Public model
│   │   └── usecase/
│   │       └── <FeatureUseCase>.kt           # Public interface
│   └── build.gradle.kts
└── impl/                   # Implementation
    ├── di/
    │   └── <Feature>Module.kt         # Koin DI module
    ├── data/
    │   └── repository/
    │       └── <FeatureRepository>Impl.kt       # Public interface implementation
    ├── domain/
    │   └── usecase/
    │       └── <FeatureUseCase>Impl.kt       # Public interface implementation
    └── build.gradle.kts
```

The tools can have any other layers that display any action.

**Example Instruments:**
- `instruments/llm/config` - Config for LLM Providers (WIP)

### Dependency Injection (Koin)

All features and modules use Koin for DI. Modules are organized by feature:
- `SharedModule` - Core shared dependencies (legacy)
- `<Feature>Module` - Feature-specific dependencies
- `<Instrument>Module` - Instrument-specific dependencies
- `<Name>Module` - Module-specific dependencies (such as `networkModule`)

Platform-specific DI files:
- `PlatformModule.android.kt`
- `PlatformModule.jvm.kt`

Modules are created as variables (`val`). To obtain the required dependencies, use `get()` inside the constructor or function.

### Database (Room)

Room is used for multiplatform persistence with these entities (legacy):
- `ChatHistoryEntity` - Chat conversation history
- `DialogEntity` - Dialog sessions
- `MessageEntity` - Individual messages
- `ReminderTaskEntity` - Scheduled reminder tasks
- `ReminderResultEntity` - Reminder execution results
- `SummaryEntity` - Generated summaries
- `McpServerEntity` - MCP server configurations

Database setup is platform-specific via `DatabaseFactory`.

## Key Technologies

- **Kotlin**: 2.2.21
- **Compose Multiplatform**: 1.9.3
- **Ktor Client**: 3.3.3 (networking)
- **Ktor Server**: 3.3.3 (backend)
- **Room**: 2.8.4 (database)
- **Koin**: 4.1.1 (dependency injection)
- **Kotlinx Coroutines**: 1.10.2
- **Kotlinx Serialization**: 1.9.0
- **DataStore**: Preferences storage
- **Detekt**: 1.23.8 (static analysis)

## Project Conventions

### API/Impl Separation

Core modules, instruments and features use API/Impl split:
- `api/` module contains public interfaces and contracts
- `impl/` module contains platform-specific implementations
- This enables loose coupling and internal encapsulation

### Layer Architecture (Clean Architecture)

The codebase follows strict layer separation:
- **Presentation** (`features/.../presentation/`) - UI, ViewModels, Compose screens
- **Domain** (`features/.../domain/`) - Use cases, business logic, repository interfaces
- **Data** (`shared/.../data/`) - Repository implementations, API clients, database

**Dependency Rules:**
- Presentation → Domain ✓
- Domain → Data ✗ (Domain only depends on interfaces, not implementations)
- Data → Domain ✓ (Data implements Domain interfaces)
- Presentation → Data ✗ (Use Domain abstractions instead)

The build includes tasks to validate these rules and highlight violations in dependency graphs.

### Naming Conventions

- **Use Cases**: `<Action><Entity>UseCase` (interface) + `<Action><Entity>UseCaseImpl` (implementation)
  - Example: `SendMessageUseCase`, `SendMessageUseCaseImpl`
- **ViewModels**: `<Feature>ViewModel` extending `MviViewModel`
- **Events/Intents**: `<Feature>Event` (sealed interface)
- **State**: `<Feature>State` (data class)
- **Effects**: `<Feature>Effect` (sealed interface)
- **DTOs**: End with `DTO` suffix (e.g., `ChatMessageDTO`)
- **Entities**: End with `Entity` suffix (e.g., `MessageEntity`)

### State Management

All UI state is managed through MVI:
- State is **immutable** (data classes)
- State updates are **pure functions** (Reducer)
- Side effects are handled separately (Effect flow)
- ViewModels expose `StateFlow<State>` and `Flow<Effect>`

### Error Handling

WIP

### Platform-Specific Code

Place platform-specific implementations in:
- `src/androidMain/kotlin/` - Android-specific
- `src/jvmMain/kotlin/` - JVM/Desktop-specific
- `src/iosMain/kotlin/` - iOS-specific
- `src/commonMain/kotlin/` - Shared across all platforms

Use `expect`/`actual` declarations for platform abstractions.

## Working with Gradle

### Composite Build

The project uses composite build with `build-logic/` containing:
- `jvm.library.gradle.kts` - JVM library conventions
- `kmp.library.gradle.kts` - Kotlin Multiplatform library conventions
- `lint.gradle.kts` - Detekt configuration
- `tools.jdeps.gradle.kts` - Dependency analysis tools

### Module Discovery

Modules are auto-discovered from target roots defined in `settings.gradle.kts`. New modules with `build.gradle.kts` files are automatically included.

### Configuration Cache

The project uses Gradle configuration cache (`org.gradle.configuration-cache=true`). Some analysis tasks disable it for stability.

### JVM Settings

- Target: Java 21
- Max heap: 8GB (`org.gradle.jvmargs=-Xmx8G`)
- Kotlin daemon: 8GB (`kotlin.daemon.jvmargs=-Xmx8G`)

## RAG System (legacy)

The `rag/` module contains a Retrieval-Augmented Generation system:
- `doc-indexer/` - Document indexing and embedding generation
  - `core/` - Core indexing logic
  - `app/` - CLI application
  - `fs-jvm/` - Filesystem utilities (JVM)
  - `ollama/` - Ollama integration for embeddings
- `rag-embeddings-data/` - Generated embeddings storage

## MCP Servers (legacy)

Model Context Protocol server implementations in `instances/servers/mcp/`:
- `primary/` - Primary MCP server
- `secondary/` - Secondary MCP server
- `testdata/` - Test projects for MCP

Each server is a standalone Ktor application.
