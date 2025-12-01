# Core Network Module - Detailed Design Document (Revised)

## 1. Overview

This document describes the detailed design and implementation plan for the **Core Transport Layer** of the network module, based on the architectural plan in `Plan.md`.

### Philosophy: Core as Primitives Only

The **core** module contains **only essential primitives**:
- Configuration classes
- Factory pattern
- Serialization formats
- Security primitives
- Error mapping
- Request/Response contexts
- Interceptor interfaces (contracts only)

**All additional functionality** (logging, metrics, auth, cache, etc.) is implemented as **plugins** in separate modules (`plugins/`).

### Current Status (Updated: 2025-12-02)

The Core Transport Layer is **60% complete** (3/5 phases done). Core primitives (Factory, Serialization, Security) are implemented. Metrics plugin and testing remain pending.

## 2. Gap Analysis

### 2.1 What is Already Implemented

#### API Module (`core/network/core/api`)
- ✅ `CoreHttpClient` - main transport interface
- ✅ Configuration classes:
  - `NetworkConfig` - global configuration
  - `LoggingConfig` - logging settings (used by logging plugin)
  - `SecurityConfig` - security settings (headers, certificate pinning interface)
  - `SerializationConfig` - JSON serialization settings
  - `TimeoutConfig` - timeout settings
- ✅ Interceptor interfaces (contracts):
  - `RequestInterceptor` - request modification
  - `ResponseInterceptor` - response modification
  - `ErrorInterceptor` - error observation
- ✅ `ErrorMapper` - error mapping interface
- ✅ Request/Response models:
  - `RequestContext` - request representation
  - `RequestOptions` - per-request overrides
  - `ResponseContext` - response representation
  - `RequestBody` - request body (Text, Bytes, Json)
  - `HttpMethod` - HTTP methods enum
- ✅ `CertificatePinning` - certificate pinning interface

#### Implementation Module (`core/network/core/impl`)
- ✅ `CoreHttpClientImpl` - Ktor-based implementation
- ✅ Platform-specific engine factories (Android/iOS/JVM)
- ✅ `DefaultErrorMapper` - basic error mapping
- ✅ `MergedConfig` - config merging logic

### 2.2 Implementation Status of Core Primitives

#### 1. Factory Pattern (Plan.md §5.2) ✅ COMPLETED (2025-11-30)
- ✅ `HttpClientFactory` - public factory interface for creating clients
  - File: `core/api/factory/HttpClientFactory.kt`
- ✅ `HttpClientFactoryImpl` - Factory implementation with proper DI integration
  - File: `core/impl/factory/HttpClientFactoryImpl.kt`

#### 2. Serialization Enhancements (Plan.md §12) ✅ COMPLETED (2025-11-30)
- ✅ `ContentFormat` - enum for JSON/ProtoBuf/Text/Binary/HTML
  - File: `core/api/serialization/ContentFormat.kt`
- ✅ `RequestBody.Multipart` - multipart/form-data support (RFC 2388 compliant)
  - File: `core/api/request/RequestBody.kt`
- ✅ `RequestBody.Stream` - file streaming support
  - File: `core/api/request/RequestBody.kt`
- ✅ Plain text/HTML response handlers in `CoreHttpClientImpl`
- ⚠️ ProtoBuf support in `CoreHttpClientImpl` - **OPTIONAL** (requires kotlinx-serialization-protobuf dependency, skipped for now)

#### 3. Security Implementation (Plan.md §9.1, §5.1) ✅ MOSTLY COMPLETED (2025-12-01)
- ✅ `CertificatePinningImpl` - SHA-256 hash-based validation
  - File: `core/impl/security/CertificatePinningImpl.kt`
- ✅ Application of `trustedCertificatesPem` from `SecurityConfig` (Android/JVM)
  - Files: `core/impl/engine/PlatformEngine.android.kt`, `PlatformEngine.jvm.kt`
- ✅ Custom certificate handling in engine config (Android OkHttp, JVM CIO)
- ⚠️ iOS certificate handling - **PREPARED** but requires platform-specific Kotlin/Native implementation
  - File: `core/impl/engine/PlatformEngine.ios.kt` (TODO comments in place)

#### 4. Enhanced Request Context ✅ COMPLETED (2025-11-30)
- ✅ `body` field added to `RequestContext`
  - File: `core/api/request/RequestContext.kt`
- ✅ `RequestOptions.body` deprecated in favor of `RequestContext.body`
  - File: `core/api/request/RequestOptions.kt`

#### 5. Dependency Injection (Plan.md §15.1) ✅ COMPLETED (2025-11-30)
- ✅ `CoreNetworkModule` - Koin module for DI
  - File: `core/impl/di/CoreNetworkModule.kt`
- ✅ Factory registrations (HttpClientFactory, NetworkConfig singletons)

### 2.3 What Should be Moved to Plugins

The following are **NOT core primitives** and should be implemented as plugins:

#### ❌ Logging (Move to optional plugin or REST/WebSocket/SSE layers)
- Logging interceptors can be implemented at higher layers (REST, WebSocket, SSE)
- Core doesn't need logging - upper layers can add logging via interceptors
- `LoggingConfig` stays in core (as a primitive), but implementation is in plugins/upper layers

#### ❌ Metrics (Move to `plugins/metrics`)
- `NetworkMetrics` interface → `plugins/metrics/api`
- `NoOpNetworkMetrics` → `plugins/metrics/impl`
- `DefaultNetworkMetrics` → `plugins/metrics/impl`

## 3. Revised Core Component Design

### 3.1 Core Primitives Only

The core module provides **only** the following primitives:

1. **Transport Interface**: `CoreHttpClient`
2. **Configuration**: `NetworkConfig`, `LoggingConfig`, `SecurityConfig`, `SerializationConfig`, `TimeoutConfig`
3. **Factory**: `HttpClientFactory`
4. **Serialization**: `ContentFormat`, `RequestBody` (with Multipart, Stream)
5. **Security**: `CertificatePinning`, `CertificatePinningImpl`
6. **Error Mapping**: `ErrorMapper`, `DefaultErrorMapper`
7. **Contexts**: `RequestContext`, `ResponseContext`, `RequestOptions`
8. **Interceptor Contracts**: `RequestInterceptor`, `ResponseInterceptor`, `ErrorInterceptor`
9. **DI Module**: `CoreNetworkModule`

### 3.2 Factory Pattern

#### API: `core/network/core/api/factory/HttpClientFactory.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.core.api.factory

import ru.izhxx.aichallenge.core.network.core.api.CoreHttpClient
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper

/**
 * Factory for creating CoreHttpClient instances.
 *
 * Allows creating clients with:
 * - Global configuration
 * - Custom interceptors and mappers
 *
 * Note: Does NOT take NetworkMetrics (moved to plugins).
 */
interface HttpClientFactory {
    /**
     * Creates a new CoreHttpClient with the given configuration.
     *
     * @param config Global network configuration
     * @param requestInterceptors Global request interceptors (executed for all requests)
     * @param responseInterceptors Global response interceptors (executed for all responses)
     * @param errorMappers Global error mappers (fallback to DefaultErrorMapper)
     * @param errorInterceptors Global error observers
     * @return Configured CoreHttpClient instance
     */
    fun create(
        config: NetworkConfig = NetworkConfig.Default,
        requestInterceptors: List<RequestInterceptor> = emptyList(),
        responseInterceptors: List<ResponseInterceptor> = emptyList(),
        errorMappers: List<ErrorMapper> = emptyList(),
        errorInterceptors: List<ErrorInterceptor> = emptyList()
    ): CoreHttpClient
}
```

#### Implementation: `core/network/core/impl/factory/HttpClientFactoryImpl.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.core.impl.factory

import ru.izhxx.aichallenge.core.network.core.api.CoreHttpClient
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.impl.CoreHttpClientImpl
import ru.izhxx.aichallenge.core.network.core.impl.engine.defaultEngineFactory
import ru.izhxx.aichallenge.core.network.core.impl.mapper.DefaultErrorMapper

/**
 * Default implementation of HttpClientFactory.
 *
 * Creates CoreHttpClient instances with Ktor backend.
 */
internal class HttpClientFactoryImpl : HttpClientFactory {
    override fun create(
        config: NetworkConfig,
        requestInterceptors: List<RequestInterceptor>,
        responseInterceptors: List<ResponseInterceptor>,
        errorMappers: List<ErrorMapper>,
        errorInterceptors: List<ErrorInterceptor>
    ): CoreHttpClient {
        // Always include DefaultErrorMapper as fallback
        val finalErrorMappers = buildList {
            addAll(errorMappers)
            if (none { it is DefaultErrorMapper }) {
                add(DefaultErrorMapper())
            }
        }

        return CoreHttpClientImpl(
            baseConfig = config,
            globalRequestInterceptors = requestInterceptors,
            globalResponseInterceptors = responseInterceptors,
            globalErrorMappers = finalErrorMappers,
            globalErrorInterceptors = errorInterceptors,
            engineFactory = defaultEngineFactory()
        )
    }
}
```

### 3.3 Serialization Enhancements

#### API: `core/network/core/api/serialization/ContentFormat.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.core.api.serialization

/**
 * Supported content formats for request/response serialization.
 */
enum class ContentFormat(val mimeType: String) {
    /** JSON serialization (kotlinx.serialization) */
    JSON("application/json"),

    /** ProtoBuf serialization (kotlinx.serialization protobuf) */
    PROTOBUF("application/protobuf"),

    /** Plain text (no serialization) */
    TEXT("text/plain"),

    /** Binary data (raw bytes) */
    BINARY("application/octet-stream"),

    /** HTML content */
    HTML("text/html");

    companion object {
        /**
         * Parse ContentFormat from MIME type string.
         */
        fun fromMimeType(mimeType: String): ContentFormat? {
            val normalized = mimeType.substringBefore(';').trim().lowercase()
            return entries.find { it.mimeType.equals(normalized, ignoreCase = true) }
        }
    }
}
```

#### API: Enhanced `RequestBody` with Multipart and Stream

Update `core/network/core/api/request/RequestBody.kt`:

```kotlin
package ru.izhxx.aichallenge.core.network.core.api.request

/**
 * Абстракция тела HTTP-запроса без зависимости от Ktor.
 * Конкретное применение/кодирование выполняется в impl-слое.
 */
sealed interface RequestBody {
    /** MIME-тип содержимого, например "application/json; charset=utf-8". */
    val contentType: String?

    data class Bytes(
        val bytes: ByteArray,
        override val contentType: String? = null
    ) : RequestBody {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Bytes
            if (!bytes.contentEquals(other.bytes)) return false
            if (contentType != other.contentType) return false
            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + (contentType?.hashCode() ?: 0)
            return result
        }
    }

    data class Text(
        val text: String,
        override val contentType: String? = "text/plain; charset=utf-8"
    ) : RequestBody

    data class Json(
        val jsonString: String,
        override val contentType: String = "application/json; charset=utf-8"
    ) : RequestBody

    /**
     * Multipart form data (for file uploads).
     *
     * @param parts List of multipart parts
     * @param boundary Multipart boundary (auto-generated if null)
     */
    data class Multipart(
        val parts: List<Part>,
        val boundary: String? = null
    ) : RequestBody {
        override val contentType: String
            get() = "multipart/form-data; boundary=${boundary ?: "----KotlinMultipartBoundary"}"

        /**
         * A single part in multipart request.
         */
        sealed interface Part {
            val name: String

            data class FormField(
                override val name: String,
                val value: String
            ) : Part

            data class FileData(
                override val name: String,
                val filename: String,
                val contentType: String,
                val bytes: ByteArray
            ) : Part {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other == null || this::class != other::class) return false
                    other as FileData
                    if (name != other.name) return false
                    if (filename != other.filename) return false
                    if (contentType != other.contentType) return false
                    if (!bytes.contentEquals(other.bytes)) return false
                    return true
                }

                override fun hashCode(): Int {
                    var result = name.hashCode()
                    result = 31 * result + filename.hashCode()
                    result = 31 * result + contentType.hashCode()
                    result = 31 * result + bytes.contentHashCode()
                    return result
                }
            }
        }
    }

    /**
     * Streaming body for large files (avoids loading entire content into memory).
     *
     * Platform-specific implementation required.
     */
    data class Stream(
        val contentLength: Long?,
        override val contentType: String?,
        val provider: suspend () -> ByteArray // TODO: Platform-specific channel/stream abstraction
    ) : RequestBody
}
```

### 3.4 Enhanced RequestContext with Body

#### API: Update `core/network/core/api/request/RequestContext.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.core.api.request

/**
 * Непривязанное к реализации представление запроса для core-интерцепторов/логики.
 * Не содержит типов Ktor.
 *
 * Иммутабельный объект; изменения вносятся через [copy].
 */
data class RequestContext(
    /** Базовый URL (например, https://api.example.com). */
    val baseUrl: String,
    /** Относительный путь (например, /v1/users). */
    val path: String,
    /** HTTP-метод. */
    val method: HttpMethod,
    /**
     * Заголовки запроса. Ключи — без учёта регистра на уровне применения.
     * Здесь храним как есть, редактирование/маскирование — на уровне лог-политик.
     */
    val headers: Map<String, String> = emptyMap(),
    /**
     * Query-параметры. Значение может быть null (флаг-параметры без значения).
     */
    val query: Map<String, String?> = emptyMap(),
    /**
     * Тело запроса (опционально).
     */
    val body: RequestBody? = null,
    /**
     * Момент начала исполнения запроса (мс, unix time), может быть заполнен impl-слоем.
     */
    val startTimestampMillis: Long? = null
)
```

### 3.5 Security Implementation

#### Implementation: `core/network/core/impl/security/CertificatePinningImpl.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.core.impl.security

import ru.izhxx.aichallenge.core.network.core.api.pinning.CertificatePinning

/**
 * Simple hash-based certificate pinning implementation.
 *
 * Validates certificates by comparing SHA-256 fingerprints.
 *
 * @param pins Map of host -> list of valid SHA-256 hashes (hex format)
 */
class CertificatePinningImpl(
    private val pins: Map<String, List<String>>
) : CertificatePinning {
    override fun isPinValid(host: String, certSha256: String): Boolean {
        val validHashes = pins[host] ?: return true // No pinning configured for this host
        return certSha256.lowercase() in validHashes.map { it.lowercase() }
    }

    companion object {
        /**
         * Creates empty pinning (all certificates accepted).
         */
        fun empty(): CertificatePinning = CertificatePinningImpl(emptyMap())
    }
}
```

**Note**: Platform-specific SSL/TLS configuration with custom certificates requires Ktor engine configuration, which varies by platform. This will be implemented in platform-specific engine setup.

### 3.6 Dependency Injection

#### Implementation: `core/network/core/impl/di/CoreNetworkModule.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.core.impl.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.core.impl.factory.HttpClientFactoryImpl

/**
 * Koin module for Core Network layer.
 *
 * Provides:
 * - NetworkConfig (singleton, can be overridden)
 * - HttpClientFactory (singleton)
 */
val coreNetworkModule: Module = module {
    // Global network configuration (can be overridden by providing NetworkConfig before this module)
    single { NetworkConfig.Default }

    // HttpClient factory
    single<HttpClientFactory> { HttpClientFactoryImpl() }
}
```

### 3.7 Updated CoreHttpClientImpl

The current `CoreHttpClientImpl` needs updates to:
1. **Remove NetworkMetrics** (moved to plugins)
2. **Support enhanced RequestBody** (Multipart, Stream)
3. **Support ProtoBuf** serialization
4. **Use body from RequestContext** (not just RequestOptions)

#### Key Changes:

```kotlin
// Remove NetworkMetrics parameter
internal class CoreHttpClientImpl(
    private val baseConfig: NetworkConfig,
    private val globalRequestInterceptors: List<RequestInterceptor> = emptyList(),
    private val globalResponseInterceptors: List<ResponseInterceptor> = emptyList(),
    private val globalErrorMappers: List<ErrorMapper> = listOf(DefaultErrorMapper()),
    private val globalErrorInterceptors: List<ErrorInterceptor> = emptyList(),
    // metrics removed
    engineFactory: HttpClientEngineFactory<*> = defaultEngineFactory()
) : CoreHttpClient {
    // ... implementation
}
```

Full implementation will be provided in the implementation phase.

## 4. Plugin Architecture

Since we moved Metrics out of core, we need to define the plugin architecture.

### 4.1 Plugins as Separate Modules

```
core/network/plugins/
├── metrics/
│   ├── api/          # NetworkMetrics interface
│   └── impl/         # NoOpNetworkMetrics, DefaultNetworkMetrics
├── auth/             # Authentication plugin
│   ├── api/          # Auth interfaces
│   └── impl/         # Bearer, API Key, OAuth implementations
└── cache/            # Caching plugin
    ├── api/          # Cache interfaces
    └── impl/         # ETag, Last-Modified implementations
```

### 4.2 Metrics Plugin (Moved from Core)

**API**: `plugins/metrics/api/src/commonMain/kotlin/.../NetworkMetrics.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.plugins.metrics.api

import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Network metrics collection interface.
 *
 * Implementations can track requests and export metrics to external systems.
 */
interface NetworkMetrics {
    /**
     * Called when request starts.
     */
    fun onRequestStart(request: RequestContext)

    /**
     * Called when request completes successfully.
     */
    fun onRequestSuccess(request: RequestContext, response: ResponseContext)

    /**
     * Called when request fails.
     */
    fun onRequestError(request: RequestContext, error: AppError)
}
```

**Implementation**: `plugins/metrics/impl/src/commonMain/kotlin/.../NoOpNetworkMetrics.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.plugins.metrics.impl

import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext
import ru.izhxx.aichallenge.core.network.plugins.metrics.api.NetworkMetrics

/**
 * No-op metrics implementation.
 */
object NoOpNetworkMetrics : NetworkMetrics {
    override fun onRequestStart(request: RequestContext) = Unit
    override fun onRequestSuccess(request: RequestContext, response: ResponseContext) = Unit
    override fun onRequestError(request: RequestContext, error: AppError) = Unit
}
```

**Implementation**: `plugins/metrics/impl/src/commonMain/kotlin/.../MetricsInterceptors.kt`

```kotlin
package ru.izhxx.aichallenge.core.network.plugins.metrics.impl

import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext
import ru.izhxx.aichallenge.core.network.plugins.metrics.api.NetworkMetrics
import ru.izhxx.aichallenge.core.foundation.error.AppError

/**
 * Request interceptor that tracks request start.
 */
class MetricsRequestInterceptor(
    private val metrics: NetworkMetrics
) : RequestInterceptor {
    override suspend fun intercept(request: RequestContext): RequestContext {
        metrics.onRequestStart(request)
        return request
    }
}

/**
 * Response interceptor that tracks successful responses.
 */
class MetricsResponseInterceptor(
    private val metrics: NetworkMetrics
) : ResponseInterceptor {
    override suspend fun intercept(request: RequestContext, response: ResponseContext): ResponseContext {
        metrics.onRequestSuccess(request, response)
        return response
    }
}

/**
 * Error interceptor that tracks errors.
 */
class MetricsErrorInterceptor(
    private val metrics: NetworkMetrics
) : ErrorInterceptor {
    override suspend fun onError(request: RequestContext, error: AppError) {
        metrics.onRequestError(request, error)
    }
}
```

**Usage Example**:

```kotlin
// In DataSource module
val factory: HttpClientFactory = get()
val metrics: NetworkMetrics = get() // from metrics plugin

val client = factory.create(
    config = NetworkConfig.Default,
    requestInterceptors = listOf(MetricsRequestInterceptor(metrics)),
    responseInterceptors = listOf(MetricsResponseInterceptor(metrics)),
    errorInterceptors = listOf(MetricsErrorInterceptor(metrics))
)
```

## 5. Implementation Plan (Revised)

**Overall Progress: 3/5 Phases Complete (60%)**

### Phase 1: Core Primitives - Factory & DI ✅ **COMPLETED** (2025-11-30)
1. ✅ Create `HttpClientFactory` interface (API)
   - File: `core/network/core/api/factory/HttpClientFactory.kt`
2. ✅ Implement `HttpClientFactoryImpl` (Impl)
   - File: `core/network/core/impl/factory/HttpClientFactoryImpl.kt`
3. ✅ Create `CoreNetworkModule` for Koin DI
   - File: `core/network/core/impl/di/CoreNetworkModule.kt`
4. ✅ Update `CoreHttpClientImpl` to remove NetworkMetrics
   - File: `core/network/core/impl/CoreHttpClientImpl.kt`
5. ✅ Update `MergedConfig` to support body from RequestContext
   - File: `core/network/core/impl/config/MergedConfig.kt`

**Build Status:** ✅ BUILD SUCCESSFUL

### Phase 2: Core Primitives - Serialization ✅ **COMPLETED** (2025-11-30)
1. ✅ Create `ContentFormat` enum
   - File: `core/network/core/api/serialization/ContentFormat.kt`
2. ✅ Enhance `RequestBody` with Multipart and Stream
   - File: `core/network/core/api/request/RequestBody.kt` (updated)
3. ✅ Update `RequestContext` to include body field
   - File: `core/network/core/api/request/RequestContext.kt` (updated)
4. ✅ Deprecate `RequestOptions.body`
   - File: `core/network/core/api/request/RequestOptions.kt` (updated)
5. ✅ Update `CoreHttpClientImpl` to handle all RequestBody types
   - File: `core/network/core/impl/CoreHttpClientImpl.kt` (updated)
   - Includes support for Multipart (RFC 2388 compliant) and Stream
6. ⚠️ Add ProtoBuf support (OPTIONAL - requires dependency, skipped for now)

**Build Status:** ✅ BUILD SUCCESSFUL

### Phase 3: Core Primitives - Security ✅ **COMPLETED** (2025-12-01)
1. ✅ Implement `CertificatePinningImpl`
   - File: `core/network/core/impl/security/CertificatePinningImpl.kt`
   - SHA-256 hash-based validation with host-specific pins
2. ✅ Platform-specific SSL/TLS configuration (Android/JVM)
   - Android: `core/network/core/impl/src/androidMain/kotlin/.../engine/PlatformEngine.android.kt`
   - JVM: `core/network/core/impl/src/jvmMain/kotlin/.../engine/PlatformEngine.jvm.kt`
   - iOS: `core/network/core/impl/src/iosMain/kotlin/.../engine/PlatformEngine.ios.kt` (partial)
3. ✅ Apply `trustedCertificatesPem` in engine config
   - Android: PEM parsing with CertificateFactory, custom KeyStore, SSLContext
   - JVM: PEM parsing with CertificateFactory, custom TrustManager
   - iOS: Infrastructure prepared, requires Kotlin/Native implementation

**Build Status:** ✅ BUILD SUCCESSFUL
**Note:** iOS certificate handling requires platform-specific Kotlin/Native work (future task)

### Phase 4: Move Metrics to Plugin ⏳ **PENDING** (Priority: HIGH)
1. ❌ Create `plugins/metrics/api` module
   - Target: `core/network/plugins/metrics/api/build.gradle.kts` (exists, no source files)
2. ❌ Create `NetworkMetrics` interface in plugin API
   - Target: `core/network/plugins/metrics/api/src/commonMain/.../NetworkMetrics.kt`
3. ❌ Create `plugins/metrics/impl` module
   - Target: `core/network/plugins/metrics/impl/build.gradle.kts` (exists, no source files)
4. ❌ Implement `NoOpNetworkMetrics` in plugin Impl
   - Target: `core/network/plugins/metrics/impl/src/commonMain/.../NoOpNetworkMetrics.kt`
5. ❌ Create metrics interceptors
   - Target: `core/network/plugins/metrics/impl/src/commonMain/.../MetricsInterceptors.kt`
   - `MetricsRequestInterceptor`, `MetricsResponseInterceptor`, `MetricsErrorInterceptor`
6. ❌ Create DI module for metrics
   - Target: `core/network/plugins/metrics/impl/src/commonMain/.../di/MetricsModule.kt`

### Phase 5: Testing ⏳ **PENDING** (Priority: MEDIUM)
1. ❌ Unit tests for factory
2. ❌ Unit tests for serialization (Multipart, Stream)
3. ❌ Unit tests for security (CertificatePinning)
4. ❌ Integration tests with MockEngine
5. ❌ Platform-specific tests

## 6. Breaking Changes

### 6.1 NetworkMetrics Removed from Core

**Before** (core includes metrics):
```kotlin
val client = CoreHttpClientImpl(
    baseConfig = config,
    metrics = myMetrics // ❌ No longer available
)
```

**After** (metrics as plugin via interceptors):
```kotlin
val factory: HttpClientFactory = get()
val metrics: NetworkMetrics = get() // from metrics plugin

val client = factory.create(
    config = config,
    requestInterceptors = listOf(MetricsRequestInterceptor(metrics)),
    responseInterceptors = listOf(MetricsResponseInterceptor(metrics)),
    errorInterceptors = listOf(MetricsErrorInterceptor(metrics))
)
```

### 6.2 RequestContext.body Addition

**Before**:
```kotlin
val request = RequestContext(
    baseUrl = "https://api.example.com",
    path = "/users",
    method = HttpMethod.POST
)

val options = RequestOptions(
    body = RequestBody.Json("{\"name\": \"John\"}")
)

client.execute(request, options)
```

**After**:
```kotlin
val request = RequestContext(
    baseUrl = "https://api.example.com",
    path = "/users",
    method = HttpMethod.POST,
    body = RequestBody.Json("{\"name\": \"John\"}")
)

client.execute(request)
```

### 6.3 Factory Pattern Introduction

**Before**:
```kotlin
val client = CoreHttpClientImpl(
    baseConfig = NetworkConfig.Default,
    // ... many parameters
)
```

**After**:
```kotlin
val factory: HttpClientFactory = get() // from Koin
val client = factory.create(
    config = NetworkConfig.Default
)
```

## 7. Dependencies

### Core Module Dependencies

#### For ProtoBuf support (optional):
```kotlin
// core/network/core/impl/build.gradle.kts (commonMain)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.9.0")
implementation("io.ktor:ktor-serialization-kotlinx-protobuf:3.3.3")
```

### Metrics Plugin Dependencies

```kotlin
// plugins/metrics/impl/build.gradle.kts (commonMain)
api(projects.core.network.core.api)
api(projects.core.network.plugins.metrics.api)
implementation(libs.kotlinx.coroutinesCore)
```

## 8. Files to Create/Modify

### Core API Module (`core/network/core/api`)

**New files:**
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/api/factory/HttpClientFactory.kt`
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/api/serialization/ContentFormat.kt`

**Modified files:**
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/api/request/RequestBody.kt` - Add Multipart and Stream
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/api/request/RequestContext.kt` - Add body field
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/api/request/RequestOptions.kt` - Deprecate body field

**Deleted files:**
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/api/metrics/NetworkMetrics.kt` - **MOVE TO** `plugins/metrics/api`

### Core Implementation Module (`core/network/core/impl`)

**New files:**
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/impl/factory/HttpClientFactoryImpl.kt`
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/impl/security/CertificatePinningImpl.kt`
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/impl/di/CoreNetworkModule.kt`

**Modified files:**
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/impl/CoreHttpClientImpl.kt` - Remove metrics, enhance body handling
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/impl/MergedConfig.kt` - Support body from RequestContext
- `build.gradle.kts` - Add ProtoBuf dependencies (optional)

**Deleted files:**
- `src/commonMain/kotlin/ru/izhxx/aichallenge/core/network/core/impl/metrics/NoOpNetworkMetrics.kt` - **MOVE TO** `plugins/metrics/impl`

### Metrics Plugin (`plugins/metrics`)

**New modules:**
- `plugins/metrics/api/` - NetworkMetrics interface
- `plugins/metrics/impl/` - NoOpNetworkMetrics, DefaultNetworkMetrics, interceptors

**Files:**
- `plugins/metrics/api/src/commonMain/kotlin/.../NetworkMetrics.kt`
- `plugins/metrics/impl/src/commonMain/kotlin/.../NoOpNetworkMetrics.kt`
- `plugins/metrics/impl/src/commonMain/kotlin/.../MetricsInterceptors.kt`
- `plugins/metrics/impl/src/commonMain/kotlin/.../di/MetricsModule.kt`

## 9. Known Limitations (Updated: 2025-12-02)

### 9.1 iOS Certificate Handling
**Status:** ⚠️ Prepared but incomplete

**Current State:**
- Configuration hooks and infrastructure in place
- TODO comments document integration points
- Graceful fallback to system trust store works

**What's Missing:**
- Platform-specific PEM certificate parsing for Darwin
- SecCertificate integration (requires Kotlin/Native interop)
- CertificatePinner.Builder integration
- SHA-256 hash extraction from PEM

**Recommendation:**
Implement as separate task requiring:
- Kotlin/Native expertise
- Objective-C interop knowledge
- Testing on actual iOS devices

### 9.2 ProtoBuf Serialization
**Status:** ⚠️ Optional - not implemented

**Current State:**
- `ContentFormat.PROTOBUF` enum value defined
- Engine configurations only support JSON

**What's Missing:**
- ProtoBuf ContentNegotiation plugin installation
- Dependencies: `kotlinx-serialization-protobuf`, `ktor-serialization-kotlinx-protobuf`

**Recommendation:**
Add only when there's a concrete use case requiring binary serialization.

### 9.3 Metrics Plugin
**Status:** ❌ Module structure exists, no source files

**Current State:**
- `plugins/metrics/api/build.gradle.kts` and `impl/build.gradle.kts` exist
- No Kotlin source files in either module

**What's Missing:**
- Complete implementation (see Phase 4 of Implementation Plan)

**Recommendation:**
Highest priority for next development phase.

### 9.4 Testing Coverage
**Status:** ❌ No tests

**Current State:**
- Core implementation compiles and builds successfully
- No unit tests, integration tests, or platform-specific tests

**What's Missing:**
- Complete test suite (see Phase 5 of Implementation Plan)

**Recommendation:**
Add tests in parallel with Metrics plugin implementation.

## 10. Next Steps - Revised Implementation Order

### Strategy: Clients First → Plugins → Comprehensive Testing

**Rationale:**
- Clients provide core functionality and immediate value
- Plugins add optional features on top of working clients
- Comprehensive testing covers entire network module at the end

---

### Phase 1: REST Client Layer (Weeks 1-2) - Priority: CRITICAL

**Goal:** Enable type-safe REST API consumption

**Tasks:**
- Type-safe REST API client
- DSL for request building (`restRequest { }`)
- Automatic JSON serialization/deserialization
- Integration with CoreHttpClient via factory

**Deliverables:**
- Functional REST client with DSL
- Basic error handling
- Integration with existing APIs

**Timeline:** 1-2 weeks

---

### Phase 2: Connectivity Monitor (Week 3, parallel to Phase 1) - Priority: HIGH

**Goal:** Enable network state observation

**Tasks:**
- Platform-specific network state monitoring
- Android: ConnectivityManager
- iOS: NWPathMonitor (via Kotlin/Native)
- JVM: NetworkInterface polling

**Deliverables:**
- Working connectivity monitor on all platforms
- StateFlow-based reactive API
- Integration examples

**Timeline:** 1 week

---

### Phase 3: WebSocket Client Layer (Weeks 4-5) - Priority: HIGH

**Goal:** Enable real-time bidirectional communication

**Tasks:**
- Bidirectional real-time communication
- Connection lifecycle management
- Auto-reconnection with exponential backoff
- Ping/pong heartbeat
- Message queuing during disconnect

**Deliverables:**
- Functional WebSocket client
- Reconnection logic
- Connection state management

**Timeline:** 2 weeks

---

### Phase 4: SSE Client Layer (Week 6) - Priority: MEDIUM

**Goal:** Enable server-to-client streaming

**Tasks:**
- Server-Sent Events (unidirectional streaming)
- Event parsing (id, event, data, retry)
- Last-Event-Id support for resuming
- Auto-reconnect with configurable retry

**Deliverables:**
- Functional SSE client
- Event parsing and resuming
- Integration for LLM streaming

**Timeline:** 1 week

---

### Phase 5: Metrics Plugin (Week 7) - Priority: HIGH

**Goal:** Enable observability for all clients

**Tasks:**
- Implement `NetworkMetrics` interface
- Create `NoOpNetworkMetrics` and `DefaultNetworkMetrics`
- Implement metrics interceptors (Request, Response, Error)
- Add Koin DI module
- Platform-specific metrics export

**Deliverables:**
- Working metrics plugin
- Integration with all clients
- Metrics collection examples

**Timeline:** 1 week

---

### Phase 6: Auth Plugin (Week 8) - Priority: HIGH

**Goal:** Enable authentication for all clients

**Tasks:**
- Bearer token authentication
- API key authentication
- OAuth 2.0 flow support
- Token refresh logic

**Deliverables:**
- Working auth plugin
- Integration with REST client
- Token management

**Timeline:** 1 week

---

### Phase 7: Cache Plugin (Week 9) - Priority: MEDIUM

**Goal:** Enable caching for REST client

**Tasks:**
- ETag support
- Last-Modified support
- In-memory cache
- Optional disk cache

**Deliverables:**
- Working cache plugin
- Cache invalidation logic
- Performance improvements

**Timeline:** 1 week

---

### Phase 8: Comprehensive Network Module Testing (Weeks 10-11) - Priority: CRITICAL

**Goal:** Achieve 80%+ test coverage for entire network module

**Scope:**
- **Core Layer:**
  - Unit tests for factory, serialization, security
  - Integration tests with MockEngine
  - Platform-specific tests (Android/JVM/iOS)

- **Client Layer:**
  - REST client: Request/response cycle, DSL, error handling
  - WebSocket: Connection lifecycle, reconnection, message queuing
  - SSE: Event parsing, Last-Event-Id, reconnection
  - Connectivity: Platform-specific monitoring

- **Plugin Layer:**
  - Metrics: Interceptor chain, collection, export
  - Auth: Token management, refresh, various auth types
  - Cache: ETag/Last-Modified, invalidation

- **Integration Tests:**
  - Client + Plugin combinations
  - Real API integration (httpbin.org, echo.websocket.org)
  - Platform-specific scenarios
  - Error scenarios and edge cases

**Deliverables:**
- 80%+ code coverage for network module
- Automated test suite
- Performance benchmarks
- Test documentation

**Timeline:** 2 weeks

---

### Phase 9: Documentation & Examples (Week 12) - Priority: MEDIUM

**Goal:** Complete documentation and migration guides

**Tasks:**
- API documentation for all modules
- Integration examples (client + plugin combinations)
- Migration guides from legacy code
- Performance benchmarks and optimization tips
- Troubleshooting guides

**Deliverables:**
- Complete API documentation
- Example projects
- Migration guide
- Best practices document

**Timeline:** 1 week

---

**Total Estimated Timeline:** 12 weeks (3 months)

**For detailed client implementation plan, see [CLIENTS_PLAN.md](./CLIENTS_PLAN.md).**

## 11. Summary (Updated: 2025-12-02)

This revised design follows the **"Core as Primitives Only"** philosophy:

### ✅ Core Contains:
- Configuration classes
- Factory pattern
- Serialization formats
- Security primitives
- Error mapping
- Request/Response contexts
- Interceptor contracts

### ❌ Core Does NOT Contain:
- Logging implementation (use AppLogger directly or in upper layers)
- Metrics implementation (moved to `plugins/metrics`)
- Auth logic (moved to `plugins/auth`)
- Cache logic (moved to `plugins/cache`)

### Benefits:
- **Simpler core** - easier to maintain and test
- **Modular plugins** - optional features can be added/removed
- **Direct logging** - no need for adapter interface, use AppLogger directly
- **Clear separation** - primitives vs features
- **Better testability** - minimal dependencies in core

The implementation plan is split into 5 phases for incremental development and testing.
