# Network Module

Multiplatform network layer для AIChallenge проекта.

## 📁 Структура модуля

```
core/network/
├── README.md                    # Этот файл
├── Plan.md                      # Архитектурный план (на русском)
├── CORE_DESIGN.md              # Детальный дизайн Core Transport Layer
├── IMPLEMENTATION_PROGRESS.md  # Текущий прогресс реализации
│
├── core/                       # Core Transport Layer (примитивы)
│   ├── api/                   # Публичные интерфейсы
│   └── impl/                  # Реализация на Ktor
│
├── clients/                    # Protocol Layer (REST, WebSocket, SSE)
│   ├── rest/                  # REST клиент (не реализовано)
│   ├── websocket/            # WebSocket клиент (не реализовано)
│   ├── sse/                  # SSE клиент (не реализовано)
│   └── connectivity/         # Network connectivity checker (не реализовано)
│
└── plugins/                    # Policy & Plugins Layer
    ├── metrics/              # Metrics plugin (в процессе)
    ├── auth/                 # Auth plugin (не реализовано)
    └── cache/                # Cache plugin (не реализовано)
```

## 🎯 Философия: Core as Primitives Only

**Core модуль содержит только примитивы:**
- Configuration classes
- Factory pattern
- Serialization formats
- Security primitives
- Error mapping
- Request/Response contexts
- Interceptor contracts

**Дополнительная функциональность вынесена в плагины:**
- Logging (используем AppLogger напрямую)
- Metrics → `plugins/metrics`
- Auth → `plugins/auth`
- Cache → `plugins/cache`

## 📊 Статус реализации

| Модуль | Статус | Прогресс |
|--------|--------|----------|
| **core/api** | ✅ Частично реализован | 80% |
| **core/impl** | ✅ Частично реализован | 75% |
| **clients/rest** | ❌ Не реализован | 0% |
| **clients/websocket** | ❌ Не реализован | 0% |
| **clients/sse** | ❌ Не реализован | 0% |
| **plugins/metrics** | ❌ Не реализован | 0% |
| **plugins/auth** | ❌ Не реализован | 0% |
| **plugins/cache** | ❌ Не реализован | 0% |

**Общий прогресс:** 2/5 фаз Core Transport Layer (40%)

Подробности: [IMPLEMENTATION_PROGRESS.md](./IMPLEMENTATION_PROGRESS.md)

## 🚀 Quick Start

### 1. Добавить зависимость

```kotlin
// В вашем модуле build.gradle.kts
dependencies {
    implementation(projects.core.network.core.api)
    implementation(projects.core.network.core.impl)
}
```

### 2. Настроить DI (Koin)

```kotlin
import ru.izhxx.aichallenge.core.network.core.impl.di.coreNetworkModule

// В вашем Koin setup
startKoin {
    modules(
        coreNetworkModule,
        // ... other modules
    )
}
```

### 3. Использовать фабрику

```kotlin
import ru.izhxx.aichallenge.core.network.core.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig

// Inject factory
val factory: HttpClientFactory = get()

// Create client
val client = factory.create(
    config = NetworkConfig.Default
)

// Make request
val request = RequestContext(
    baseUrl = "https://api.example.com",
    path = "/users",
    method = HttpMethod.GET
)

val result = client.execute(request)
```

## 📝 Core Components

### Configuration

```kotlin
val config = NetworkConfig(
    serialization = SerializationConfig.DefaultStrict,
    timeouts = TimeoutConfig.Default,
    logging = LoggingConfig.ProductionSafe,
    security = SecurityConfig(
        defaultSecurityHeaders = mapOf(
            "User-Agent" to "AIChallenge/1.0",
            "X-Platform" to "Android"
        )
    )
)
```

### Request Body Types

```kotlin
// JSON
val jsonBody = RequestBody.Json("""{"name": "John"}""")

// Text
val textBody = RequestBody.Text("Hello World")

// Binary
val bytesBody = RequestBody.Bytes(byteArrayOf(0x01, 0x02))

// Multipart (file upload)
val multipartBody = RequestBody.Multipart(
    parts = listOf(
        RequestBody.Multipart.Part.FormField("name", "John"),
        RequestBody.Multipart.Part.FileData(
            name = "avatar",
            filename = "photo.jpg",
            contentType = "image/jpeg",
            bytes = imageBytes
        )
    )
)

// Stream (large files)
val streamBody = RequestBody.Stream(
    contentLength = 1024L,
    contentType = "application/octet-stream",
    provider = suspend { loadLargeFile() }
)
```

### Interceptors

```kotlin
// Request interceptor
val authInterceptor = RequestInterceptor { request ->
    request.copy(
        headers = request.headers + ("Authorization" to "Bearer $token")
    )
}

// Response interceptor
val loggingInterceptor = ResponseInterceptor { request, response ->
    println("${request.method} ${request.path} -> ${response.statusCode}")
    response
}

// Error interceptor
val errorInterceptor = ErrorInterceptor { request, error ->
    println("Request failed: ${error.message}")
}

// Create client with interceptors
val client = factory.create(
    config = config,
    requestInterceptors = listOf(authInterceptor),
    responseInterceptors = listOf(loggingInterceptor),
    errorInterceptors = listOf(errorInterceptor)
)
```

### Error Handling

```kotlin
when (val result = client.execute(request)) {
    is AppResult.Success -> {
        val response = result.data
        println("Success: ${response.statusCode}")
    }
    is AppResult.Failure -> {
        when (val error = result.error) {
            is AppError.NetworkError -> println("Network error: ${error.message}")
            is AppError.HttpError -> println("HTTP ${error.status}: ${error.message}")
            is AppError.TimeoutError -> println("Request timeout")
            is AppError.SerializationError -> println("Serialization failed")
            else -> println("Unknown error: ${error.message}")
        }
    }
}
```

## 🔧 Поддерживаемые платформы

- ✅ Android (Ktor OkHttp)
- ✅ iOS (Ktor Darwin)
- ✅ JVM/Desktop (Ktor CIO)

## 📚 Документация

- **[Plan.md](./Plan.md)** - Верхнеуровневая архитектура (русский)
- **[CORE_DESIGN.md](./CORE_DESIGN.md)** - Детальный дизайн Core Transport Layer
- **[IMPLEMENTATION_PROGRESS.md](./IMPLEMENTATION_PROGRESS.md)** - Текущий прогресс и история изменений

## 🛠️ Разработка

### Build

```bash
./gradlew :core:network:core:api:build
./gradlew :core:network:core:impl:build
```

### Tests

```bash
./gradlew :core:network:core:impl:test
```

## 🔜 Roadmap

### ✅ Completed
- [x] Phase 1: Factory & DI
- [x] Phase 2: Serialization (Multipart, Stream)

### ⏳ In Progress
- [ ] Phase 3: Security (Certificate Pinning)

### 📋 Planned
- [ ] Phase 4: Testing
- [ ] REST Client Layer
- [ ] WebSocket Client Layer
- [ ] SSE Client Layer
- [ ] Auth Plugin
- [ ] Cache Plugin
- [ ] Metrics Plugin

## 🤝 Contributing

При добавлении новых компонентов:

1. **Core примитивы** → `core/api` и `core/impl`
2. **Дополнительная функциональность** → `plugins/`
3. **Protocol-specific логика** → `clients/`

Следуйте принципу: **Core as Primitives Only**

## 📄 License

Часть AIChallenge проекта.
