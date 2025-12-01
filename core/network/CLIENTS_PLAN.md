# Network Clients Implementation Plan

**Document Version:** 1.0
**Date:** 2025-12-02
**Status:** Planning Phase

## Executive Summary

This document provides a comprehensive architectural design and phased implementation plan for four network client modules built on top of the core transport layer (`core/network/core/`):

1. **REST Client** - Type-safe REST API consumption
2. **WebSocket Client** - Bidirectional real-time communication
3. **SSE Client** - Server-Sent Events (unidirectional streaming)
4. **Connectivity Monitor** - Network state observation

**Current Status:**
- Core Transport Layer: 60% complete (Phases 1-3 done, 4-5 pending)
- All client modules: 0% (only build.gradle.kts structure exists)
- All plugin modules: 0% (empty structure)

**Total Estimated Timeline:** 8-10 weeks for complete implementation

---

## Table of Contents

1. [REST Client](#1-rest-client)
2. [WebSocket Client](#2-websocket-client)
3. [SSE Client](#3-sse-client)
4. [Connectivity Monitor](#4-connectivity-monitor)
5. [Dependencies & Integration](#5-dependencies-integration)
6. [Implementation Roadmap](#6-implementation-roadmap)
7. [Testing Strategy](#7-testing-strategy)
8. [Architectural Decisions](#8-architectural-decisions)

---

## 1. REST Client

### 1.1 Overview

**Purpose:** Type-safe HTTP REST API client with automatic serialization/deserialization.

**Use Cases:**
- CRUD operations
- RESTful API consumption
- JSON-based communication
- File uploads/downloads

### 1.2 Module Structure

```
core/network/clients/rest/
├── api/                                    # Public contracts
│   └── src/commonMain/kotlin/
│       └── ru/izhxx/aichallenge/core/network/clients/rest/api/
│           ├── RestClient.kt               # Main interface
│           ├── request/
│           │   ├── RestRequest.kt          # Request builder
│           │   ├── RestMethod.kt           # GET, POST, PUT, DELETE, PATCH
│           │   └── RestHeaders.kt          # Header builder
│           ├── response/
│           │   ├── RestResponse.kt         # Typed response wrapper
│           │   └── StatusCode.kt           # HTTP status codes enum
│           └── config/
│               └── RestConfig.kt           # REST-specific configuration
│
└── impl/                                   # Implementation
    └── src/commonMain/kotlin/
        └── ru/izhxx/aichallenge/core/network/clients/rest/impl/
            ├── RestClientImpl.kt           # Main implementation
            ├── builder/
            │   └── RestRequestBuilder.kt   # DSL implementation
            ├── serialization/
            │   └── JsonSerializer.kt       # JSON serializer
            └── di/
                └── RestClientModule.kt     # Koin DI
```

### 1.3 Key Interfaces

#### RestClient

```kotlin
interface RestClient {
    suspend fun <T> execute(
        request: RestRequest,
        responseSerializer: KSerializer<T>
    ): AppResult<RestResponse<T>>

    suspend fun executeNoContent(
        request: RestRequest
    ): AppResult<RestResponse<Unit>>

    suspend fun executeRaw(
        request: RestRequest
    ): AppResult<RestResponse<ByteArray>>
}
```

#### DSL Example

```kotlin
val request = restRequest {
    method = RestMethod.POST
    path = "/users"
    header("Authorization", "Bearer $token")
    query("page", "1")
    jsonBody(user)
}

val result = restClient.execute(request, UserList.serializer())
```

### 1.4 Integration with Core

- Uses `CoreHttpClient` via `HttpClientFactory`
- Converts `RestRequest` → `RequestContext`
- Maps `ResponseContext` → `RestResponse<T>`
- Automatic JSON serialization via kotlinx.serialization

### 1.5 Implementation Phases

**Phase 1: Foundation** (Week 1)
- Create API interfaces and data models
- Implement basic RestClientImpl
- Add Koin DI module
- Unit tests with MockEngine

**Phase 2: DSL & Features** (Week 2)
- Implement DSL builder
- Add convenience extensions
- Integration tests with real API

**Priority:** HIGH
**Timeline:** 1-2 weeks
**Dependencies:** Core factory pattern (✅ completed)

---

## 2. WebSocket Client

### 2.1 Overview

**Purpose:** Bidirectional real-time communication over WebSocket protocol.

**Use Cases:**
- Chat applications
- Live collaboration
- Gaming
- Real-time data feeds

### 2.2 Module Structure

```
core/network/clients/websocket/
├── api/
│   └── src/commonMain/kotlin/
│       └── ru/izhxx/aichallenge/core/network/clients/websocket/api/
│           ├── WebSocketClient.kt         # Main interface
│           ├── connection/
│           │   ├── WebSocketConnection.kt # Active connection handle
│           │   ├── ConnectionState.kt     # CONNECTING/CONNECTED/DISCONNECTED
│           │   └── CloseReason.kt         # Close codes
│           ├── message/
│           │   └── WebSocketMessage.kt    # Text/Binary/Ping/Pong
│           └── config/
│               └── WebSocketConfig.kt     # WebSocket-specific config
│
└── impl/
    └── src/commonMain/kotlin/
        └── ru/izhxx/aichallenge/core/network/clients/websocket/impl/
            ├── WebSocketClientImpl.kt     # Main implementation
            ├── connection/
            │   ├── DefaultWebSocketConnection.kt
            │   ├── ConnectionManager.kt   # Lifecycle management
            │   └── ReconnectionStrategy.kt # Auto-reconnect logic
            └── di/
                └── WebSocketModule.kt     # Koin DI
```

### 2.3 Key Interfaces

#### WebSocketClient

```kotlin
interface WebSocketClient {
    suspend fun connect(
        url: String,
        config: WebSocketConfig = WebSocketConfig.Default
    ): AppResult<WebSocketConnection>

    val events: Flow<WebSocketEvent>
}
```

#### WebSocketConnection

```kotlin
interface WebSocketConnection {
    val state: StateFlow<ConnectionState>
    val messages: Flow<WebSocketMessage>

    suspend fun send(message: WebSocketMessage): AppResult<Unit>
    suspend fun sendText(text: String): AppResult<Unit>
    suspend fun sendBinary(bytes: ByteArray): AppResult<Unit>
    suspend fun close(reason: CloseReason = CloseReason.NormalClosure)
}
```

### 2.4 Integration with Core

**Note:** WebSocket uses Ktor's WebSocket plugin, NOT CoreHttpClient (different protocol).

- Shares configuration patterns with core
- Uses same error handling (`AppResult`)
- Platform engines: OkHttp (Android), NSURLSession (iOS), CIO (JVM)

### 2.5 Implementation Phases

**Phase 1: Basic Connection** (Week 1)
- Create API interfaces
- Implement basic connection (no reconnect)
- Send/receive text messages
- Add Koin DI

**Phase 2: Lifecycle & Features** (Week 2)
- State management
- Auto-reconnection with exponential backoff
- Ping/pong heartbeat
- Message queuing during reconnect

**Priority:** HIGH
**Timeline:** 2 weeks
**Dependencies:** None (independent of core)

---

## 3. SSE Client

### 3.1 Overview

**Purpose:** Server-Sent Events client for unidirectional server-to-client streaming.

**Use Cases:**
- LLM streaming responses
- Live feeds (news, stock tickers)
- Notifications
- Real-time updates

### 3.2 Module Structure

```
core/network/clients/sse/
├── api/
│   └── src/commonMain/kotlin/
│       └── ru/izhxx/aichallenge/core/network/clients/sse/api/
│           ├── SseClient.kt               # Main interface
│           ├── connection/
│           │   ├── SseConnection.kt       # Active SSE stream
│           │   └── ConnectionState.kt     # Connection states
│           ├── event/
│           │   └── ServerSentEvent.kt     # SSE event structure
│           └── config/
│               └── SseConfig.kt           # SSE-specific config
│
└── impl/
    └── src/commonMain/kotlin/
        └── ru/izhxx/aichallenge/core/network/clients/sse/impl/
            ├── SseClientImpl.kt           # Main implementation
            ├── connection/
            │   ├── DefaultSseConnection.kt
            │   └── SseEventParser.kt      # Parse SSE format
            ├── retry/
            │   └── RetryStrategy.kt       # Reconnection with Last-Event-Id
            └── di/
                └── SseModule.kt           # Koin DI
```

### 3.3 Key Interfaces

#### SseClient

```kotlin
interface SseClient {
    suspend fun connect(
        url: String,
        config: SseConfig = SseConfig.Default
    ): AppResult<SseConnection>
}
```

#### SseConnection

```kotlin
interface SseConnection {
    val state: StateFlow<ConnectionState>
    val events: Flow<ServerSentEvent>
    val lastEventId: StateFlow<String?>

    suspend fun close()
}

data class ServerSentEvent(
    val id: String? = null,
    val event: String = "message",
    val data: String,
    val retry: Long? = null
)
```

### 3.4 Integration with Core

- HTTP-based (uses `text/event-stream` content type)
- Uses Ktor SSE plugin
- Builds on HTTP streaming capabilities
- Supports Last-Event-Id header for resuming

### 3.5 Implementation Phases

**Phase 1: Basic Streaming** (Week 1)
- Create API interfaces
- Implement SSE parsing
- Connect and receive events
- Add Koin DI
- Last-Event-Id support
- Auto-reconnect logic

**Priority:** MEDIUM
**Timeline:** 1 week
**Dependencies:** None (uses Ktor SSE plugin)

---

## 4. Connectivity Monitor

### 4.1 Overview

**Purpose:** Platform-specific network connectivity monitoring.

**Use Cases:**
- Detect online/offline state
- Show offline UI
- Pause/resume network operations
- Handle metered connections

### 4.2 Module Structure

```
core/network/clients/connectivity/
├── api/
│   └── src/commonMain/kotlin/
│       └── ru/izhxx/aichallenge/core/network/clients/connectivity/api/
│           ├── ConnectivityMonitor.kt     # Main interface
│           ├── state/
│           │   ├── NetworkState.kt        # Network availability state
│           │   ├── ConnectionType.kt      # WiFi, Cellular, Ethernet
│           │   └── NetworkCapabilities.kt # Speed, metered
│
└── impl/
    └── src/
        ├── commonMain/kotlin/
        │   └── .../impl/
        │       └── di/ConnectivityModule.kt
        ├── androidMain/kotlin/
        │   └── .../impl/AndroidConnectivityMonitor.kt  # ConnectivityManager
        ├── iosMain/kotlin/
        │   └── .../impl/IosConnectivityMonitor.kt      # NWPathMonitor
        └── jvmMain/kotlin/
            └── .../impl/JvmConnectivityMonitor.kt      # NetworkInterface
```

### 4.3 Key Interfaces

#### ConnectivityMonitor

```kotlin
interface ConnectivityMonitor {
    val networkState: StateFlow<NetworkState>
    val isConnected: Boolean

    suspend fun startMonitoring()
    suspend fun stopMonitoring()
}

data class NetworkState(
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val capabilities: NetworkCapabilities? = null
)

enum class ConnectionType {
    NONE, WIFI, CELLULAR, ETHERNET, VPN, UNKNOWN
}
```

### 4.4 Platform-Specific Implementations

**Android:**
- `ConnectivityManager` with `NetworkCallback`
- Supports Android 5.0+ (API 21+)
- Real-time network state changes

**iOS:**
- `NWPathMonitor` (Network framework)
- Requires Kotlin/Native interop
- Real-time path monitoring

**Desktop JVM:**
- `NetworkInterface.getNetworkInterfaces()`
- Polling-based (every 5 seconds)
- Basic connectivity detection

### 4.5 Implementation Phases

**Phase 1: Basic Detection** (Week 1)
- Create API interfaces
- Implement Android monitor (ConnectivityManager)
- Implement iOS monitor (NWPathMonitor)
- Implement JVM monitor (NetworkInterface polling)
- Add Koin DI with expect/actual

**Priority:** MEDIUM
**Timeline:** 1 week
**Dependencies:** None (independent)

---

## 5. Dependencies & Integration

### 5.1 Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                        Application                           │
└────────────────┬──────────────┬──────────────┬──────────────┘
                 │              │              │
                 v              v              v
         ┌──────────────┐ ┌──────────┐ ┌──────────────┐
         │ REST Client  │ │ WebSocket│ │  SSE Client  │
         └──────┬───────┘ └────┬─────┘ └──────┬───────┘
                │              │              │
                v              │              v
         ┌──────────────┐      │       ┌──────────────┐
         │ CoreHttpClient│     │       │ Ktor SSE     │
         └──────┬───────┘      │       └──────────────┘
                │              │
                v              v
         ┌──────────────────────────────┐
         │   Ktor HttpClient Engine     │
         └──────────────────────────────┘
                │
                v
         ┌──────────────────────────────┐
         │     Platform Network APIs    │
         │  (OkHttp/NSURLSession/CIO)   │
         └──────────────────────────────┘

         ┌──────────────────────────────┐
         │  Connectivity Monitor        │ (Independent)
         │  (Android/iOS/JVM specific)  │
         └──────────────────────────────┘
```

### 5.2 Plugin Integration Examples

#### REST Client + Auth Plugin

```kotlin
val authPlugin = BearerAuthInterceptor(tokenProvider)
val restClient = factory.create(
    requestInterceptors = listOf(authPlugin)
)
```

#### REST Client + Metrics Plugin

```kotlin
val metrics: NetworkMetrics = get()
val restClient = factory.create(
    requestInterceptors = listOf(MetricsRequestInterceptor(metrics)),
    responseInterceptors = listOf(MetricsResponseInterceptor(metrics)),
    errorInterceptors = listOf(MetricsErrorInterceptor(metrics))
)
```

#### All Clients + Connectivity Monitor

```kotlin
class NetworkAwareRepository(
    private val restClient: RestClient,
    private val connectivityMonitor: ConnectivityMonitor
) {
    init {
        connectivityMonitor.networkState.collect { state ->
            if (!state.isConnected) {
                // Switch to cached data
            }
        }
    }
}
```

---

## 6. Implementation Roadmap

### Strategy: Clients First → Plugins → Comprehensive Testing

**Rationale:**
- Clients provide immediate functional value
- Plugins enhance existing client functionality
- Comprehensive testing validates entire network stack

**Timeline:** 12 weeks total

---

### Phase 1: REST Client (Weeks 1-2)
**Status:** 🚀 Ready to start
**Priority:** CRITICAL

**Week 1: API & Basic Implementation**
1. Create all API interfaces (RestClient, RestRequest, RestResponse, StatusCode)
2. Implement RestClientImpl with CoreHttpClient integration
3. Add Koin DI module (RestClientModule)
4. Write basic unit tests with MockEngine

**Week 2: DSL & Features**
1. Implement DSL builder (RestRequestBuilder, `restRequest {}`)
2. Add convenience extensions (inline reified functions)
3. Implement error mapping (HTTP codes → AppError)
4. Integration tests with real API (httpbin.org)

**Deliverables:**
- ✅ Functional REST client with type-safe API
- ✅ DSL for request building
- ✅ JSON serialization/deserialization
- ✅ Basic error handling
- ✅ Integration with existing APIs

**Blockers:** None (core factory pattern already implemented)

**Success Criteria:**
- Can make GET/POST/PUT/DELETE requests
- Automatic JSON serialization works
- DSL is intuitive and type-safe
- Basic tests pass

---

### Phase 2: Connectivity Monitor (Week 3)
**Status:** 🚀 Ready to start (can run parallel to Phase 1)
**Priority:** HIGH

**Tasks:**
1. Create API interfaces (ConnectivityMonitor, NetworkState, ConnectionType)
2. Implement platform-specific monitors:
   - **Android:** ConnectivityManager with NetworkCallback
   - **iOS:** NWPathMonitor via Kotlin/Native interop
   - **JVM:** NetworkInterface polling (5-second interval)
3. Add Koin DI with expect/actual pattern
4. Platform-specific tests

**Deliverables:**
- ✅ Working connectivity monitor on all platforms
- ✅ StateFlow-based reactive API
- ✅ Connection type detection (WiFi, Cellular, Ethernet)
- ✅ Integration examples

**Blockers:** None (independent module)

**Success Criteria:**
- Real-time network state updates on Android/iOS
- Polling works on Desktop
- StateFlow emits on network changes

---

### Phase 3: WebSocket Client (Weeks 4-5)
**Status:** ⏸️ Waiting for Phase 1-2
**Priority:** HIGH

**Week 4: Basic Connection**
1. Create API interfaces (WebSocketClient, WebSocketConnection, WebSocketMessage)
2. Implement connection lifecycle (CONNECTING → CONNECTED → DISCONNECTED)
3. Text message send/receive
4. State management with StateFlow
5. Graceful close with CloseReason

**Week 5: Advanced Features**
1. Auto-reconnection strategy with exponential backoff
2. Ping/pong heartbeat (30-second interval)
3. Binary message support
4. Message queuing during reconnection
5. Connection timeout handling

**Deliverables:**
- ✅ Functional WebSocket client
- ✅ Reconnection logic with configurable strategy
- ✅ Message queue during disconnect
- ✅ Heartbeat mechanism
- ✅ State management

**Blockers:** None (uses Ktor WebSocket plugin directly)

**Success Criteria:**
- Can connect to wss:// endpoints
- Auto-reconnects on disconnect
- Message queue works during reconnection
- Heartbeat prevents idle timeout

---

### Phase 4: SSE Client (Week 6)
**Status:** ⏸️ Waiting for Phase 3
**Priority:** MEDIUM

**Tasks:**
1. Create API interfaces (SseClient, SseConnection, ServerSentEvent)
2. Implement SSE parsing (id, event, data, retry fields)
3. Add Last-Event-Id support for resuming streams
4. Auto-reconnect logic with server-provided retry interval
5. Integration with LLM streaming use case (if applicable)

**Deliverables:**
- ✅ Functional SSE client
- ✅ Event parsing (multiline data support)
- ✅ Resume support with Last-Event-Id
- ✅ Auto-reconnect with configurable retry

**Blockers:** None (uses Ktor SSE plugin)

**Success Criteria:**
- Can receive SSE streams
- Parses all SSE fields correctly
- Resumes from last event on reconnect
- Works with LLM streaming APIs

---

### Phase 5: Metrics Plugin (Week 7)
**Status:** ⏸️ Waiting for all clients
**Priority:** HIGH

**Tasks:**
1. Create `plugins/metrics/api` module
   - `NetworkMetrics` interface (onRequestStart, onRequestSuccess, onRequestError)
2. Create `plugins/metrics/impl` module
   - `NoOpNetworkMetrics` (no-op implementation)
   - `DefaultNetworkMetrics` (in-memory counters)
   - `MetricsRequestInterceptor`
   - `MetricsResponseInterceptor`
   - `MetricsErrorInterceptor`
3. Add Koin DI module (MetricsModule)
4. Platform-specific metrics export (Android: StatsD, iOS: OSLog, JVM: JMX)
5. Integration with all clients

**Deliverables:**
- ✅ Working metrics plugin
- ✅ Integration with REST/WebSocket/SSE clients
- ✅ Metrics collection and export
- ✅ Examples for each client

**Blockers:** Needs all clients implemented

**Success Criteria:**
- Metrics collected for all requests
- Can export to platform-specific systems
- Low overhead (< 1ms per request)

---

### Phase 6: Auth Plugin (Week 8)
**Status:** ⏸️ Waiting for Phase 5
**Priority:** HIGH

**Tasks:**
1. Create `plugins/auth/api` module
   - `AuthProvider` interface
   - `AuthToken` data model
2. Create `plugins/auth/impl` module
   - `BearerAuthInterceptor` (Authorization: Bearer <token>)
   - `ApiKeyAuthInterceptor` (API key in header/query)
   - `OAuth2AuthProvider` (OAuth 2.0 flow)
   - `TokenRefreshStrategy` (auto-refresh before expiry)
3. Add Koin DI module (AuthModule)
4. Integration with REST client

**Deliverables:**
- ✅ Working auth plugin with multiple strategies
- ✅ Integration with REST client
- ✅ Token refresh logic
- ✅ Secure token storage examples

**Blockers:** Needs REST client

**Success Criteria:**
- Bearer auth works
- Token refresh automatic
- OAuth 2.0 flow complete

---

### Phase 7: Cache Plugin (Week 9)
**Status:** ⏸️ Waiting for Phase 6
**Priority:** MEDIUM

**Tasks:**
1. Create `plugins/cache/api` module
   - `CacheStorage` interface
   - `CachePolicy` configuration
2. Create `plugins/cache/impl` module
   - `ETagCacheInterceptor` (ETag header support)
   - `LastModifiedCacheInterceptor` (Last-Modified header)
   - `InMemoryCacheStorage` (LRU cache)
   - `DiskCacheStorage` (optional, platform-specific)
   - `CacheInvalidationStrategy`
3. Add Koin DI module (CacheModule)
4. Integration with REST client

**Deliverables:**
- ✅ Working cache plugin
- ✅ ETag and Last-Modified support
- ✅ In-memory cache with LRU eviction
- ✅ Cache invalidation logic
- ✅ Performance improvements (50%+ cache hit rate)

**Blockers:** Needs REST client

**Success Criteria:**
- ETag validation works
- Cache reduces network calls
- Invalidation works correctly

---

### Phase 8: Comprehensive Network Module Testing (Weeks 10-11)
**Status:** ⏸️ Waiting for all implementations
**Priority:** CRITICAL

**Goal:** Achieve 80%+ test coverage for entire network module

**Week 10: Core & Client Testing**

**Core Layer Tests:**
- Factory pattern (HttpClientFactory creation)
- Serialization (Multipart, Stream, ContentFormat)
- Security (CertificatePinning validation)
- Platform engines (Android/iOS/JVM)
- MockEngine integration tests

**Client Layer Tests:**
- **REST Client:**
  - Request/response cycle (all HTTP methods)
  - DSL builder correctness
  - JSON serialization/deserialization
  - Error handling (4xx, 5xx codes)
  - Integration with httpbin.org
- **WebSocket Client:**
  - Connection lifecycle (connect, disconnect, reconnect)
  - Message send/receive (text, binary)
  - Reconnection with exponential backoff
  - Message queuing during disconnect
  - Heartbeat mechanism
  - Integration with echo.websocket.org
- **SSE Client:**
  - Event parsing (all fields)
  - Multiline data support
  - Last-Event-Id resuming
  - Reconnection with retry interval
  - Integration with test SSE server
- **Connectivity Monitor:**
  - Android: ConnectivityManager callback
  - iOS: NWPathMonitor updates
  - JVM: NetworkInterface polling
  - StateFlow emission on changes

**Week 11: Plugin & Integration Testing**

**Plugin Layer Tests:**
- **Metrics Plugin:**
  - Interceptor chain execution
  - Metric collection accuracy
  - Platform-specific export
  - Low overhead validation
- **Auth Plugin:**
  - Bearer token injection
  - API key injection
  - OAuth 2.0 flow
  - Token refresh logic
- **Cache Plugin:**
  - ETag validation
  - Last-Modified validation
  - Cache hit/miss logic
  - LRU eviction
  - Invalidation strategies

**Integration Tests:**
- REST + Auth + Metrics
- REST + Cache + Metrics
- WebSocket + Metrics
- SSE + Metrics
- All clients + Connectivity Monitor
- Real API scenarios (httpbin.org, echo.websocket.org)
- Platform-specific scenarios
- Error scenarios and edge cases
- Performance benchmarks

**Deliverables:**
- ✅ 80%+ code coverage for entire network module
- ✅ Automated test suite (JUnit, XCTest)
- ✅ Performance benchmarks (latency, throughput)
- ✅ Test documentation
- ✅ CI/CD integration

**Success Criteria:**
- All unit tests pass
- Integration tests pass on all platforms
- Performance meets targets (< 10ms overhead)
- Coverage > 80%

---

### Phase 9: Documentation & Examples (Week 12)
**Status:** ⏸️ Final phase
**Priority:** MEDIUM

**Tasks:**

**API Documentation:**
- KDoc for all public interfaces
- Module-level documentation
- Architecture diagrams

**Integration Examples:**
- REST client basic usage
- REST + Auth + Cache example
- WebSocket real-time chat example
- SSE LLM streaming example
- Connectivity-aware repository pattern

**Migration Guides:**
- From legacy HTTP client to REST client
- From manual WebSocket to WebSocket client
- Plugin adoption guide

**Performance Documentation:**
- Benchmarks for each client
- Optimization tips
- Resource usage guidelines

**Troubleshooting Guides:**
- Common errors and solutions
- Platform-specific issues
- Debug logging setup

**Deliverables:**
- ✅ Complete API documentation
- ✅ Example projects (3-5 samples)
- ✅ Migration guide
- ✅ Best practices document
- ✅ Troubleshooting guide

**Success Criteria:**
- All public APIs documented
- Examples compile and run
- Migration path is clear

---

## Timeline Summary

| Phase | Weeks | Priority | Status |
|-------|-------|----------|--------|
| 1. REST Client | 1-2 | CRITICAL | 🚀 Ready |
| 2. Connectivity Monitor | 3 | HIGH | 🚀 Ready (parallel) |
| 3. WebSocket Client | 4-5 | HIGH | ⏸️ Waiting |
| 4. SSE Client | 6 | MEDIUM | ⏸️ Waiting |
| 5. Metrics Plugin | 7 | HIGH | ⏸️ Waiting |
| 6. Auth Plugin | 8 | HIGH | ⏸️ Waiting |
| 7. Cache Plugin | 9 | MEDIUM | ⏸️ Waiting |
| 8. Comprehensive Testing | 10-11 | CRITICAL | ⏸️ Waiting |
| 9. Documentation | 12 | MEDIUM | ⏸️ Waiting |

**Total:** 12 weeks (3 months)

---

## 7. Testing Strategy

### 7.1 Unit Tests

**REST Client:**
- MockEngine for HTTP mocking
- Test serialization/deserialization
- Test error handling
- Test DSL builder

**WebSocket Client:**
- Mock WebSocket session
- Test connection lifecycle
- Test reconnection logic
- Test message queuing

**SSE Client:**
- Mock SSE stream
- Test event parsing
- Test Last-Event-Id
- Test auto-reconnect

**Connectivity Monitor:**
- Platform-specific test doubles
- Mock network state changes
- Test StateFlow emissions

### 7.2 Integration Tests

**REST Client:**
- Against real test API (httpbin.org)
- Test full request/response cycle
- Test error scenarios

**WebSocket Client:**
- Against echo.websocket.org
- Test bidirectional communication
- Test reconnection

**SSE Client:**
- Against test SSE server
- Test streaming events
- Test reconnection with Last-Event-Id

### 7.3 Platform Tests

**Android:**
- Instrumented tests on emulator
- Test ConnectivityManager integration

**iOS:**
- XCTest on simulator
- Test NWPathMonitor integration

**Desktop:**
- JUnit on JVM
- Test NetworkInterface polling

---

## 8. Architectural Decisions

### 8.1 Why Separate Clients?

Each protocol has unique characteristics:

- **REST:** HTTP request/response, stateless, uses CoreHttpClient
- **WebSocket:** Full-duplex, stateful, separate Ktor plugin
- **SSE:** HTTP streaming, stateful, text/event-stream

Attempting to unify would create a leaky abstraction. Clean separation allows:
- Protocol-specific optimizations
- Clear API contracts
- Independent testing
- Modular dependencies

### 8.2 Error Handling Strategy

All clients return `AppResult<T>` for consistency:
- **Success:** `AppResult.Success(value)`
- **Failure:** `AppResult.Error(appError)`

Error mapping:
- Network errors → `NetworkError` (from core)
- Serialization errors → `SerializationError`
- Protocol errors → Protocol-specific errors

### 8.3 Concurrency Model

All clients use structured concurrency:
- **Scopes:** Each connection manages its own CoroutineScope
- **Cancellation:** Closing connection cancels all child coroutines
- **Backpressure:** Flow-based APIs handle backpressure naturally

### 8.4 Platform Abstraction

Use `expect`/`actual` for platform-specific code:
- **Connectivity Monitor:** Completely platform-specific
- **REST/WebSocket/SSE:** Shared logic, platform-specific engines

---

## Summary

### Implementation Order: Clients → Plugins → Testing

**Phase 1-4: Clients** (Weeks 1-6)
1. **REST Client** - Priority: CRITICAL (Weeks 1-2)
2. **Connectivity Monitor** - Priority: HIGH (Week 3, parallel to REST)
3. **WebSocket Client** - Priority: HIGH (Weeks 4-5)
4. **SSE Client** - Priority: MEDIUM (Week 6)

**Phase 5-7: Plugins** (Weeks 7-9)
5. **Metrics Plugin** - Priority: HIGH (Week 7)
6. **Auth Plugin** - Priority: HIGH (Week 8)
7. **Cache Plugin** - Priority: MEDIUM (Week 9)

**Phase 8-9: Testing & Documentation** (Weeks 10-12)
8. **Comprehensive Testing** - Priority: CRITICAL (Weeks 10-11)
   - Core layer tests
   - Client layer tests
   - Plugin layer tests
   - Integration tests
   - Performance benchmarks
9. **Documentation** - Priority: MEDIUM (Week 12)
   - API documentation
   - Examples
   - Migration guides

---

### Why This Order?

**Clients First:**
- Provide immediate functional value
- Enable real-world usage early
- Can be tested independently

**Plugins Second:**
- Enhance existing client functionality
- Require clients to be implemented first
- Add optional features progressively

**Testing Last:**
- Validates entire network stack holistically
- Ensures all components work together
- Catches integration issues
- Provides comprehensive coverage

---

### Success Metrics

**By Week 2:** REST Client functional, teams can consume APIs
**By Week 3:** Network monitoring available on all platforms
**By Week 6:** All clients operational, real-time features available
**By Week 9:** All plugins implemented, full feature set available
**By Week 11:** 80%+ test coverage, production-ready quality
**By Week 12:** Complete documentation, migration path clear

---

**Total Estimated Timeline:** 12 weeks (3 months)

**Next Action:** Start Phase 1 (REST Client implementation)
