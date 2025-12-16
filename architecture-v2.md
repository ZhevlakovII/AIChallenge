# Архитектура LLM Toolkit v2

## Обзор

Мультиплатформенный toolkit для работы с LLM, MCP и RAG. Построен на принципах SOLID, KISS, DRY с максимальным переиспользованием кода и независимостью фич.

**Платформы:** Android, iOS, Desktop (JVM), Backend (JVM)

**Технологии:** Kotlin Multiplatform, Compose Multiplatform, Ktor, Room, Koin, KotlinX (Coroutines, Serialization, DateTime)

---

## Слои архитектуры

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            APPS (targets)                               │
│      :app:android    :app:ios    :app:desktop    :app:shared            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        FEATURES (api + impl)                            │
│                                                                         │
│   :feature:chat:api        :feature:summarizer:api                      │
│          ▲                          ▲                                   │
│          │                          │                                   │
│   :feature:chat:impl       :feature:summarizer:impl                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         TOOLS (domain modules)                          │
│            :tool:llm          :tool:mcp          :tool:rag              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           CORE (shared base)                            │
│   :core:common   :core:model   :core:ui:navigation   :core:network      │
│                                                      :core:ui:mvi       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Структура проекта

```
project/
├── build-logic/
│   └── convention/                    # Gradle convention plugins
│       ├── src/main/kotlin/
│       │   ├── KmpLibraryPlugin.kt
│       │   ├── KmpComposePlugin.kt
│       │   ├── FeatureApiPlugin.kt
│       │   └── FeatureImplPlugin.kt
│       └── build.gradle.kts
│
├── core/
│   ├── common/                        # Базовые утилиты
│   ├── model/                         # Общие доменные модели
│   ├── navigation/                    # NavigationBus, интенты
│   ├── network/                       # Ktor client
│   ├── database/                      # Room setup
│   └── ui/                            # Compose UI kit
│
├── tool/
│   ├── llm/                           # LLM клиент
│   ├── mcp/                           # MCP клиент + сервер
│   └── rag/                           # RAG pipeline
│
├── feature/
│   ├── chat/
│   │   ├── api/                       # Навигационные контракты
│   │   └── impl/                      # Реализация фичи
│   │
│   └── summarizer/
│       ├── api/
│       └── impl/
│
├── app/
│   ├── android/                       # Full Android app
│   ├── ios/                           # Full iOS app
│   ├── desktop/                       # Full Desktop app
│   └── chat-mobile/                   # Minimal chat (Android + iOS)
│
├── gradle/
│   └── libs.versions.toml             # Version catalog
│
├── settings.gradle.kts
└── build.gradle.kts
```

---

## Core модули

### :core:common

Базовые утилиты без платформенных зависимостей.

```kotlin
// Result wrapper
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
}

// Flow extensions
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .catch { emit(Result.Error(it)) }

// UseCase base
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}

// Dispatcher provider (для тестирования)
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
```

**Зависимости:** kotlinx-coroutines-core, kotlinx-datetime, kotlinx-atomicfu

---

### :core:model

Общие доменные модели, используемые между tool-модулями. Ключевой модуль для избежания зависимостей между tools.

```kotlin
// === Tool Definition (LLM + MCP) ===

data class Tool(
    val name: String,
    val description: String,
    val inputSchema: ToolSchema
)

data class ToolSchema(
    val type: String = "object",
    val properties: Map<String, PropertySchema> = emptyMap(),
    val required: List<String> = emptyList()
)

data class PropertySchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val items: PropertySchema? = null  // Для arrays
)

// === Tool Execution ===

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject
)

data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean = false
)

// === Messages ===

sealed class Message {
    abstract val role: String
    
    data class System(val content: String) : Message() {
        override val role = "system"
    }
    
    data class User(
        val content: String,
        val attachments: List<Attachment> = emptyList()
    ) : Message() {
        override val role = "user"
    }
    
    data class Assistant(
        val content: String? = null,
        val toolCalls: List<ToolCall> = emptyList()
    ) : Message() {
        override val role = "assistant"
    }
    
    data class ToolResponse(
        val toolCallId: String,
        val content: String
    ) : Message() {
        override val role = "tool"
    }
}

data class Attachment(
    val type: AttachmentType,
    val data: String,  // Base64 или URL
    val mimeType: String? = null
)

enum class AttachmentType { IMAGE, FILE }

// === Embeddings ===

@JvmInline
value class Embedding(val vector: FloatArray) {
    val dimensions: Int get() = vector.size
}

// === Streaming ===

data class StreamChunk(
    val delta: String?,
    val toolCalls: List<ToolCall>? = null,
    val finishReason: FinishReason? = null
)

enum class FinishReason {
    STOP,
    TOOL_CALLS,
    LENGTH,
    CONTENT_FILTER
}
```

**Зависимости:** :core:common, kotlinx-serialization-json

---

### :core:navigation

Децентрализованная навигация через EventBus с типизированными интентами.

```kotlin
// === Base Intent ===

interface NavigationIntent

// === Handler ===

abstract class NavigationHandler<T : NavigationIntent>(
    private val intentClass: KClass<T>
) {
    abstract fun handle(intent: T): Boolean
    
    @Suppress("UNCHECKED_CAST")
    fun tryHandle(intent: NavigationIntent): Boolean {
        return if (intentClass.isInstance(intent)) {
            handle(intent as T)
        } else false
    }
}

// Convenience для создания handlers
inline fun <reified T : NavigationIntent> navigationHandler(
    crossinline block: (T) -> Boolean
): NavigationHandler<T> = object : NavigationHandler<T>(T::class) {
    override fun handle(intent: T): Boolean = block(intent)
}

// === Navigation Bus ===

class NavigationBus {
    private val handlers = mutableListOf<NavigationHandler<*>>()
    private val pendingIntents = mutableListOf<NavigationIntent>()
    private var isReady = false
    
    fun register(handler: NavigationHandler<*>) {
        handlers.add(handler)
    }
    
    fun unregister(handler: NavigationHandler<*>) {
        handlers.remove(handler)
    }
    
    fun markReady() {
        isReady = true
        pendingIntents.forEach { send(it) }
        pendingIntents.clear()
    }
    
    fun <T : NavigationIntent> send(
        intent: T,
        fallback: (() -> Unit)? = null
    ) {
        if (!isReady) {
            pendingIntents.add(intent)
            return
        }
        
        val handled = handlers.any { it.tryHandle(intent) }
        if (!handled) {
            fallback?.invoke()
        }
    }
    
    // Проверка доступности фичи
    inline fun <reified T : NavigationIntent> canHandle(): Boolean {
        return handlers.any { handler ->
            try {
                handler.tryHandle(object : NavigationIntent {} as T)
                false  // Не тот тип
            } catch (e: ClassCastException) {
                true   // Правильный handler
            }
        }
    }
}

// === DI ===

val navigationModule = module {
    single { NavigationBus() }
}
```

**Зависимости:** :core:common, koin-core

---

### :core:network

Конфигурация HTTP клиента.

```kotlin
// === Platform Engine ===

expect class PlatformHttpEngineFactory() {
    fun create(): HttpClientEngine
}

// androidMain: OkHttp
// iosMain: Darwin
// desktopMain/jvmMain: CIO

// === Client Provider ===

class HttpClientProvider(
    private val engineFactory: PlatformHttpEngineFactory = PlatformHttpEngineFactory()
) {
    fun create(
        config: HttpClientConfig.() -> Unit = {}
    ): HttpClient = HttpClient(engineFactory.create()) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            })
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
        }
        
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
        
        config()
    }
}

// === SSE Support ===

fun HttpClient.sse(
    url: String,
    headers: Map<String, String> = emptyMap()
): Flow<ServerSentEvent> = flow {
    prepareGet(url) {
        headers.forEach { (key, value) -> header(key, value) }
        header(HttpHeaders.Accept, "text/event-stream")
    }.execute { response ->
        val channel = response.bodyAsChannel()
        val buffer = StringBuilder()
        
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            
            if (line.isEmpty()) {
                parseEvent(buffer.toString())?.let { emit(it) }
                buffer.clear()
            } else {
                buffer.appendLine(line)
            }
        }
    }
}

data class ServerSentEvent(
    val event: String?,
    val data: String,
    val id: String? = null
)

private fun parseEvent(raw: String): ServerSentEvent? {
    val lines = raw.lines()
    var event: String? = null
    var data: String? = null
    var id: String? = null
    
    lines.forEach { line ->
        when {
            line.startsWith("event:") -> event = line.removePrefix("event:").trim()
            line.startsWith("data:") -> data = line.removePrefix("data:").trim()
            line.startsWith("id:") -> id = line.removePrefix("id:").trim()
        }
    }
    
    return data?.let { ServerSentEvent(event, it, id) }
}

// === DI ===

val networkModule = module {
    single { HttpClientProvider() }
    single { get<HttpClientProvider>().create() }
}
```

**Зависимости:** :core:common, ktor-client-*, kotlinx-serialization-json

---

### :core:database

Room Multiplatform setup.

```kotlin
// === Database Builder ===

expect class DatabaseBuilder {
    fun <T : RoomDatabase> build(
        klass: KClass<T>,
        name: String,
        migrations: List<Migration> = emptyList()
    ): T
}

// === Common Converters ===

class CommonTypeConverters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilliseconds()
    
    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }
    
    @TypeConverter
    fun fromJsonObject(value: JsonObject?): String? = value?.toString()
    
    @TypeConverter
    fun toJsonObject(value: String?): JsonObject? = value?.let { Json.parseToJsonElement(it).jsonObject }
    
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? = value?.joinToString(",")
    
    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? = value?.split(",")?.map { it.toFloat() }?.toFloatArray()
}

// === Base Entity ===

interface BaseEntity {
    val id: String
    val createdAt: Instant
    val updatedAt: Instant
}

// === DI ===

val databaseModule = module {
    single { DatabaseBuilder() }
}
```

**Зависимости:** :core:common, room-runtime, room-ktx, kotlinx-datetime

---

### :core:ui

Compose UI kit для клиентских приложений.

```kotlin
// === Theme ===

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme() else dynamicLightColorScheme()
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// === Common Components ===

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorMessage(
    error: Throwable,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error.message ?: "Unknown error",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    // Markdown rendering implementation
}

// === State Wrappers ===

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error -> ErrorMessage(state.exception, onRetry)
        is UiState.Success -> content(state.data)
    }
}
```

**Зависимости:** :core:common, compose-multiplatform, compose-material3

---

## Tool модули

### :tool:llm

OpenAI-совместимый LLM клиент.

```kotlin
// === Configuration ===

data class LlmConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val defaultModel: String,
    val timeout: Duration = 60.seconds,
    val headers: Map<String, String> = emptyMap()
)

// === API ===

interface LlmClient {
    fun chat(request: ChatRequest): Flow<StreamChunk>
    suspend fun chatComplete(request: ChatRequest): ChatResponse
    suspend fun models(): List<Model>
}

data class ChatRequest(
    val model: String? = null,  // null = использовать defaultModel
    val messages: List<Message>,
    val tools: List<Tool> = emptyList(),
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val topP: Float? = null,
    val stop: List<String>? = null
)

data class ChatResponse(
    val id: String,
    val model: String,
    val message: Message.Assistant,
    val usage: Usage?
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class Model(
    val id: String,
    val ownedBy: String? = null
)

// === Implementation ===

class OpenAiCompatibleClient(
    private val httpClient: HttpClient,
    private val config: LlmConfig
) : LlmClient {
    
    override fun chat(request: ChatRequest): Flow<StreamChunk> = flow {
        httpClient.sse(
            url = "${config.baseUrl}/chat/completions",
            headers = buildHeaders()
        ).collect { event ->
            if (event.data == "[DONE]") return@collect
            
            val chunk = Json.decodeFromString<OpenAiChunk>(event.data)
            emit(chunk.toStreamChunk())
        }
    }
    
    override suspend fun chatComplete(request: ChatRequest): ChatResponse {
        val response = httpClient.post("${config.baseUrl}/chat/completions") {
            headers { appendHeaders() }
            setBody(request.toOpenAiRequest(config.defaultModel, stream = false))
        }
        return response.body<OpenAiResponse>().toChatResponse()
    }
    
    override suspend fun models(): List<Model> {
        val response = httpClient.get("${config.baseUrl}/models") {
            headers { appendHeaders() }
        }
        return response.body<ModelsResponse>().data.map { it.toModel() }
    }
    
    private fun buildHeaders(): Map<String, String> = buildMap {
        config.apiKey?.let { put("Authorization", "Bearer $it") }
        putAll(config.headers)
    }
}

// === DI ===

val llmModule = module {
    single<LlmClient> { params ->
        val config: LlmConfig = params.get()
        OpenAiCompatibleClient(get(), config)
    }
}
```

**Зависимости:** :core:common, :core:model, :core:network

---

### :tool:mcp

MCP клиент и сервер с поддержкой всех транспортов.

```kotlin
// === Transport Layer (commonMain) ===

interface McpTransport {
    val isConnected: Boolean
    suspend fun connect()
    suspend fun send(message: JsonRpcMessage)
    fun receive(): Flow<JsonRpcMessage>
    suspend fun close()
}

sealed class JsonRpcMessage {
    abstract val jsonrpc: String  // "2.0"
    
    data class Request(
        override val jsonrpc: String = "2.0",
        val id: Long,
        val method: String,
        val params: JsonObject? = null
    ) : JsonRpcMessage()
    
    data class Response(
        override val jsonrpc: String = "2.0",
        val id: Long,
        val result: JsonElement? = null,
        val error: JsonRpcError? = null
    ) : JsonRpcMessage()
    
    data class Notification(
        override val jsonrpc: String = "2.0",
        val method: String,
        val params: JsonObject? = null
    ) : JsonRpcMessage()
}

data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

// === HTTP/SSE Transport (commonMain) ===

class HttpSseTransport(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : McpTransport {
    private var messageEndpoint: String? = null
    private val _isConnected = AtomicBoolean(false)
    
    override val isConnected: Boolean get() = _isConnected.value
    
    override suspend fun connect() {
        // SSE endpoint возвращает message endpoint в первом событии
    }
    
    override fun receive(): Flow<JsonRpcMessage> = httpClient.sse("$baseUrl/sse")
        .mapNotNull { event -> 
            if (event.event == "endpoint") {
                messageEndpoint = event.data
                null
            } else {
                Json.decodeFromString<JsonRpcMessage>(event.data)
            }
        }
    
    override suspend fun send(message: JsonRpcMessage) {
        val endpoint = messageEndpoint ?: error("Not connected")
        httpClient.post(endpoint) {
            setBody(message)
        }
    }
    
    override suspend fun close() {
        _isConnected.value = false
    }
}

// === WebSocket Transport (commonMain) ===

class WebSocketTransport(
    private val httpClient: HttpClient,
    private val url: String
) : McpTransport {
    private var session: WebSocketSession? = null
    
    override suspend fun connect() {
        session = httpClient.webSocketSession(url)
    }
    
    override fun receive(): Flow<JsonRpcMessage> = flow {
        session?.incoming?.consumeAsFlow()?.collect { frame ->
            if (frame is Frame.Text) {
                emit(Json.decodeFromString(frame.readText()))
            }
        }
    }
    
    override suspend fun send(message: JsonRpcMessage) {
        session?.send(Frame.Text(Json.encodeToString(message)))
    }
    
    override suspend fun close() {
        session?.close()
        session = null
    }
}

// === Stdio Transport (jvmMain only) ===

// jvmMain/kotlin/StdioTransport.kt
class StdioTransport(
    private val command: List<String>,
    private val workingDir: File? = null,
    private val environment: Map<String, String> = emptyMap()
) : McpTransport {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    
    override val isConnected: Boolean get() = process?.isAlive == true
    
    override suspend fun connect() {
        val processBuilder = ProcessBuilder(command).apply {
            workingDir?.let { directory(it) }
            environment().putAll(this@StdioTransport.environment)
            redirectErrorStream(false)
        }
        process = processBuilder.start()
        writer = process!!.outputStream.bufferedWriter()
    }
    
    override fun receive(): Flow<JsonRpcMessage> = flow {
        val reader = process?.inputStream?.bufferedReader() ?: return@flow
        
        while (process?.isAlive == true) {
            val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
            
            if (line.startsWith("Content-Length:")) {
                val length = line.removePrefix("Content-Length:").trim().toInt()
                reader.readLine()  // Empty line
                val content = CharArray(length)
                reader.read(content)
                emit(Json.decodeFromString(String(content)))
            }
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun send(message: JsonRpcMessage) {
        val content = Json.encodeToString(message)
        val header = "Content-Length: ${content.length}\r\n\r\n"
        
        withContext(Dispatchers.IO) {
            writer?.apply {
                write(header)
                write(content)
                flush()
            }
        }
    }
    
    override suspend fun close() {
        writer?.close()
        process?.destroy()
        process = null
    }
}

// === MCP Client ===

interface McpClient {
    val isConnected: Boolean
    suspend fun connect()
    suspend fun initialize(): ServerCapabilities
    suspend fun listTools(): List<McpTool>
    suspend fun callTool(name: String, arguments: JsonObject): McpToolResult
    suspend fun listResources(): List<McpResource>
    suspend fun readResource(uri: String): McpResourceContent
    suspend fun disconnect()
}

data class McpTool(
    val name: String,
    val description: String?,
    val inputSchema: JsonObject
)

data class McpToolResult(
    val content: List<McpContent>,
    val isError: Boolean = false
)

sealed class McpContent {
    data class Text(val text: String) : McpContent()
    data class Image(val data: String, val mimeType: String) : McpContent()
    data class Resource(val uri: String, val text: String?, val mimeType: String?) : McpContent()
}

data class McpResource(
    val uri: String,
    val name: String,
    val description: String?,
    val mimeType: String?
)

data class McpResourceContent(
    val uri: String,
    val contents: List<McpContent>
)

data class ServerCapabilities(
    val tools: Boolean = false,
    val resources: Boolean = false,
    val prompts: Boolean = false
)

// === MCP Client Implementation ===

class DefaultMcpClient(
    private val transport: McpTransport
) : McpClient {
    private val requestId = AtomicLong(0)
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<JsonRpcMessage.Response>>()
    private var receiveJob: Job? = null
    
    override val isConnected: Boolean get() = transport.isConnected
    
    override suspend fun connect() {
        transport.connect()
        receiveJob = CoroutineScope(Dispatchers.Default).launch {
            transport.receive().collect { message ->
                if (message is JsonRpcMessage.Response) {
                    pendingRequests.remove(message.id)?.complete(message)
                }
            }
        }
    }
    
    override suspend fun initialize(): ServerCapabilities {
        val response = request("initialize", buildJsonObject {
            put("protocolVersion", "2024-11-05")
            putJsonObject("capabilities") {}
            putJsonObject("clientInfo") {
                put("name", "llm-toolkit")
                put("version", "1.0.0")
            }
        })
        
        // Send initialized notification
        transport.send(JsonRpcMessage.Notification(method = "notifications/initialized"))
        
        return response.result?.let { Json.decodeFromJsonElement(it) } 
            ?: ServerCapabilities()
    }
    
    override suspend fun listTools(): List<McpTool> {
        val response = request("tools/list")
        return response.result?.jsonObject?.get("tools")
            ?.let { Json.decodeFromJsonElement(it) }
            ?: emptyList()
    }
    
    override suspend fun callTool(name: String, arguments: JsonObject): McpToolResult {
        val response = request("tools/call", buildJsonObject {
            put("name", name)
            put("arguments", arguments)
        })
        return response.result?.let { Json.decodeFromJsonElement(it) }
            ?: McpToolResult(emptyList(), isError = true)
    }
    
    override suspend fun disconnect() {
        receiveJob?.cancel()
        transport.close()
    }
    
    private suspend fun request(method: String, params: JsonObject? = null): JsonRpcMessage.Response {
        val id = requestId.incrementAndGet()
        val deferred = CompletableDeferred<JsonRpcMessage.Response>()
        pendingRequests[id] = deferred
        
        transport.send(JsonRpcMessage.Request(id = id, method = method, params = params))
        
        return withTimeout(30.seconds) { deferred.await() }
    }
}

// === Conversion to Core Model ===

fun McpTool.toTool(): Tool = Tool(
    name = name,
    description = description ?: "",
    inputSchema = inputSchema.toToolSchema()
)

private fun JsonObject.toToolSchema(): ToolSchema {
    return ToolSchema(
        type = this["type"]?.jsonPrimitive?.content ?: "object",
        properties = this["properties"]?.jsonObject?.mapValues { (_, v) ->
            v.jsonObject.toPropertySchema()
        } ?: emptyMap(),
        required = this["required"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    )
}

private fun JsonObject.toPropertySchema(): PropertySchema {
    return PropertySchema(
        type = this["type"]?.jsonPrimitive?.content ?: "string",
        description = this["description"]?.jsonPrimitive?.contentOrNull,
        enum = this["enum"]?.jsonArray?.map { it.jsonPrimitive.content }
    )
}

fun McpToolResult.toToolResult(toolCallId: String): ToolResult = ToolResult(
    toolCallId = toolCallId,
    content = content.joinToString("\n") { 
        when (it) {
            is McpContent.Text -> it.text
            is McpContent.Image -> "[Image: ${it.mimeType}]"
            is McpContent.Resource -> it.text ?: "[Resource: ${it.uri}]"
        }
    },
    isError = isError
)

// === MCP Server (jvmMain only) ===

// jvmMain/kotlin/McpServer.kt
interface McpServer {
    fun registerTool(handler: McpToolHandler)
    fun registerResource(provider: McpResourceProvider)
    suspend fun start()
    suspend fun stop()
}

interface McpToolHandler {
    val definition: McpTool
    suspend fun execute(arguments: JsonObject): McpToolResult
}

interface McpResourceProvider {
    val resources: List<McpResource>
    suspend fun read(uri: String): McpResourceContent
}

class LocalMcpServer(
    private val transport: McpServerTransport
) : McpServer {
    private val tools = mutableMapOf<String, McpToolHandler>()
    private val resourceProviders = mutableListOf<McpResourceProvider>()
    
    override fun registerTool(handler: McpToolHandler) {
        tools[handler.definition.name] = handler
    }
    
    override fun registerResource(provider: McpResourceProvider) {
        resourceProviders.add(provider)
    }
    
    override suspend fun start() {
        transport.start { request ->
            handleRequest(request)
        }
    }
    
    private suspend fun handleRequest(request: JsonRpcMessage.Request): JsonElement? {
        return when (request.method) {
            "initialize" -> buildJsonObject {
                put("protocolVersion", "2024-11-05")
                putJsonObject("capabilities") {
                    if (tools.isNotEmpty()) putJsonObject("tools") {}
                    if (resourceProviders.isNotEmpty()) putJsonObject("resources") {}
                }
                putJsonObject("serverInfo") {
                    put("name", "llm-toolkit-server")
                    put("version", "1.0.0")
                }
            }
            "tools/list" -> buildJsonObject {
                putJsonArray("tools") {
                    tools.values.forEach { add(Json.encodeToJsonElement(it.definition)) }
                }
            }
            "tools/call" -> {
                val name = request.params?.get("name")?.jsonPrimitive?.content
                val args = request.params?.get("arguments")?.jsonObject ?: JsonObject(emptyMap())
                val handler = tools[name]
                handler?.execute(args)?.let { Json.encodeToJsonElement(it) }
            }
            else -> null
        }
    }
    
    override suspend fun stop() {
        transport.stop()
    }
}

// === DI ===

val mcpModule = module {
    // Transport factory
    factory<McpTransport> { (config: McpTransportConfig) ->
        when (config) {
            is McpTransportConfig.Sse -> HttpSseTransport(get(), config.baseUrl)
            is McpTransportConfig.WebSocket -> WebSocketTransport(get(), config.url)
            is McpTransportConfig.Stdio -> StdioTransport(config.command, config.workingDir, config.environment)
        }
    }
    
    factory<McpClient> { (transport: McpTransport) ->
        DefaultMcpClient(transport)
    }
}

sealed class McpTransportConfig {
    data class Sse(val baseUrl: String) : McpTransportConfig()
    data class WebSocket(val url: String) : McpTransportConfig()
    data class Stdio(
        val command: List<String>,
        val workingDir: File? = null,
        val environment: Map<String, String> = emptyMap()
    ) : McpTransportConfig()
}
```

**Зависимости:** :core:common, :core:model, :core:network

---

### :tool:rag

RAG pipeline с поддержкой локальных и удалённых эмбеддингов.

```kotlin
// === Embedding Provider ===

interface EmbeddingProvider {
    val dimensions: Int
    suspend fun embed(text: String): Embedding
    suspend fun embedBatch(texts: List<String>): List<Embedding>
}

// Remote provider (commonMain)
class RemoteEmbeddingProvider(
    private val httpClient: HttpClient,
    private val config: EmbeddingConfig
) : EmbeddingProvider {
    
    override val dimensions: Int = config.dimensions
    
    override suspend fun embed(text: String): Embedding {
        return embedBatch(listOf(text)).first()
    }
    
    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        val response = httpClient.post("${config.baseUrl}/embeddings") {
            config.apiKey?.let { header("Authorization", "Bearer $it") }
            setBody(EmbeddingRequest(model = config.model, input = texts))
        }
        
        return response.body<EmbeddingResponse>().data
            .sortedBy { it.index }
            .map { Embedding(it.embedding.toFloatArray()) }
    }
}

data class EmbeddingConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
    val dimensions: Int
)

// Local provider (jvmMain + androidMain)
// Использует ONNX Runtime для локальных моделей
expect class LocalEmbeddingProvider(modelPath: String) : EmbeddingProvider

// === Vector Store ===

interface VectorStore {
    suspend fun add(id: String, embedding: Embedding, metadata: JsonObject = JsonObject(emptyMap()))
    suspend fun addBatch(items: List<VectorEntry>)
    suspend fun search(query: Embedding, topK: Int = 10, filter: MetadataFilter? = null): List<SearchResult>
    suspend fun get(id: String): VectorEntry?
    suspend fun delete(id: String)
    suspend fun deleteByFilter(filter: MetadataFilter)
    suspend fun clear()
    suspend fun count(): Int
}

data class VectorEntry(
    val id: String,
    val embedding: Embedding,
    val metadata: JsonObject = JsonObject(emptyMap())
)

data class SearchResult(
    val id: String,
    val score: Float,
    val metadata: JsonObject
)

sealed class MetadataFilter {
    data class Equals(val key: String, val value: JsonPrimitive) : MetadataFilter()
    data class In(val key: String, val values: List<JsonPrimitive>) : MetadataFilter()
    data class And(val filters: List<MetadataFilter>) : MetadataFilter()
    data class Or(val filters: List<MetadataFilter>) : MetadataFilter()
}

// In-memory implementation
class InMemoryVectorStore : VectorStore {
    private val vectors = ConcurrentHashMap<String, VectorEntry>()
    
    override suspend fun search(
        query: Embedding, 
        topK: Int, 
        filter: MetadataFilter?
    ): List<SearchResult> {
        return vectors.values
            .filter { filter?.matches(it.metadata) ?: true }
            .map { entry ->
                SearchResult(
                    id = entry.id,
                    score = cosineSimilarity(query.vector, entry.embedding.vector),
                    metadata = entry.metadata
                )
            }
            .sortedByDescending { it.score }
            .take(topK)
    }
    
    // ... other implementations
}

private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) { "Vectors must have same dimensions" }
    
    var dotProduct = 0f
    var normA = 0f
    var normB = 0f
    
    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    
    return dotProduct / (sqrt(normA) * sqrt(normB))
}

// Persistent implementation (Room-backed)
class PersistentVectorStore(
    private val dao: VectorDao
) : VectorStore {
    // Room entity хранит embedding как String (FloatArray -> CSV)
    // Search выполняется in-memory после загрузки из БД
    // Для больших объёмов — интеграция с векторной БД
}

// === Text Chunking ===

interface TextChunker {
    fun chunk(text: String): List<TextChunk>
}

data class TextChunk(
    val content: String,
    val index: Int,
    val startOffset: Int,
    val endOffset: Int
)

class SentenceChunker(
    private val maxChunkSize: Int = 512,
    private val overlap: Int = 50
) : TextChunker {
    override fun chunk(text: String): List<TextChunk> {
        // Split by sentences, merge to target size with overlap
    }
}

class TokenChunker(
    private val maxTokens: Int = 256,
    private val overlap: Int = 20
) : TextChunker {
    // Token-based chunking (requires tokenizer)
}

// === RAG Pipeline ===

interface RagPipeline {
    suspend fun index(documents: List<Document>)
    suspend fun index(document: Document)
    suspend fun query(question: String, topK: Int = 5): RagContext
    suspend fun delete(documentId: String)
}

data class Document(
    val id: String,
    val content: String,
    val metadata: JsonObject = JsonObject(emptyMap())
)

data class RagContext(
    val chunks: List<RetrievedChunk>,
    val augmentedPrompt: String
)

data class RetrievedChunk(
    val documentId: String,
    val content: String,
    val score: Float,
    val metadata: JsonObject
)

class DefaultRagPipeline(
    private val embeddingProvider: EmbeddingProvider,
    private val vectorStore: VectorStore,
    private val chunker: TextChunker = SentenceChunker(),
    private val promptTemplate: String = DEFAULT_RAG_TEMPLATE
) : RagPipeline {
    
    override suspend fun index(document: Document) {
        val chunks = chunker.chunk(document.content)
        val embeddings = embeddingProvider.embedBatch(chunks.map { it.content })
        
        val entries = chunks.zip(embeddings).map { (chunk, embedding) ->
            VectorEntry(
                id = "${document.id}#${chunk.index}",
                embedding = embedding,
                metadata = buildJsonObject {
                    put("documentId", document.id)
                    put("chunkIndex", chunk.index)
                    put("content", chunk.content)
                    document.metadata.forEach { (key, value) -> put(key, value) }
                }
            )
        }
        
        vectorStore.addBatch(entries)
    }
    
    override suspend fun query(question: String, topK: Int): RagContext {
        val queryEmbedding = embeddingProvider.embed(question)
        val results = vectorStore.search(queryEmbedding, topK)
        
        val chunks = results.map { result ->
            RetrievedChunk(
                documentId = result.metadata["documentId"]?.jsonPrimitive?.content ?: "",
                content = result.metadata["content"]?.jsonPrimitive?.content ?: "",
                score = result.score,
                metadata = result.metadata
            )
        }
        
        val context = chunks.joinToString("\n\n") { it.content }
        val augmentedPrompt = promptTemplate
            .replace("{context}", context)
            .replace("{question}", question)
        
        return RagContext(chunks, augmentedPrompt)
    }
    
    override suspend fun delete(documentId: String) {
        vectorStore.deleteByFilter(
            MetadataFilter.Equals("documentId", JsonPrimitive(documentId))
        )
    }
    
    companion object {
        private const val DEFAULT_RAG_TEMPLATE = """
            Use the following context to answer the question.
            
            Context:
            {context}
            
            Question: {question}
            
            Answer:
        """.trimIndent()
    }
}

// === DI ===

val ragModule = module {
    // Embedding provider (выбор на основе конфигурации)
    single<EmbeddingProvider> { params ->
        val config: RagConfig = params.get()
        if (config.useLocalEmbeddings) {
            LocalEmbeddingProvider(config.localModelPath!!)
        } else {
            RemoteEmbeddingProvider(get(), config.embeddingConfig!!)
        }
    }
    
    single<VectorStore> { PersistentVectorStore(get()) }
    single<TextChunker> { SentenceChunker() }
    single<RagPipeline> { DefaultRagPipeline(get(), get(), get()) }
}

data class RagConfig(
    val useLocalEmbeddings: Boolean = false,
    val localModelPath: String? = null,
    val embeddingConfig: EmbeddingConfig? = null
)
```

**Зависимости:** :core:common, :core:model, :core:network, :core:database

---

## Feature модули

### :feature:chat:api

Навигационный контракт для Chat feature. Минимальный модуль — только интенты.

```kotlin
// === Navigation Intents ===

sealed interface ChatIntent : NavigationIntent

data class OpenChatIntent(
    val conversationId: String? = null
) : ChatIntent

data object OpenConversationsIntent : ChatIntent

data class OpenNewChatIntent(
    val systemPrompt: String? = null,
    val initialMessage: String? = null
) : ChatIntent

// === Navigation Helper ===

object ChatNavigation {
    fun openChat(bus: NavigationBus, conversationId: String? = null) {
        bus.send(OpenChatIntent(conversationId))
    }
    
    fun openConversations(bus: NavigationBus) {
        bus.send(OpenConversationsIntent)
    }
    
    fun openNewChat(
        bus: NavigationBus, 
        systemPrompt: String? = null,
        initialMessage: String? = null
    ) {
        bus.send(OpenNewChatIntent(systemPrompt, initialMessage))
    }
}
```

**Зависимости:** :core:navigation (только!)

---

### :feature:chat:impl

Полная реализация Chat feature.

```kotlin
// === Navigation Handler ===

internal class ChatNavigationHandler(
    private val navController: NavController
) : NavigationHandler<ChatIntent>(ChatIntent::class) {
    
    override fun handle(intent: ChatIntent): Boolean {
        when (intent) {
            is OpenChatIntent -> {
                val route = intent.conversationId
                    ?.let { "chat/conversation/$it" }
                    ?: "chat/conversations"
                navController.navigate(route)
            }
            is OpenConversationsIntent -> {
                navController.navigate("chat/conversations")
            }
            is OpenNewChatIntent -> {
                navController.navigate("chat/new") {
                    // Pass args via SavedStateHandle
                }
            }
        }
        return true
    }
}

// === Domain Layer ===

data class Conversation(
    val id: String,
    val title: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messageCount: Int
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val createdAt: Instant,
    val status: MessageStatus
)

enum class MessageRole { USER, ASSISTANT, SYSTEM, TOOL }
enum class MessageStatus { SENDING, SENT, STREAMING, ERROR }

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun createConversation(systemPrompt: String? = null): Conversation
    suspend fun deleteConversation(id: String)
    fun sendMessage(conversationId: String, content: String): Flow<ChatMessage>
}

// === Data Layer ===

class ChatRepositoryImpl(
    private val llmClient: LlmClient,
    private val mcpClient: McpClient?,
    private val ragPipeline: RagPipeline?,
    private val localDataSource: ChatLocalDataSource,
    private val config: ChatConfig,
    private val dispatchers: DispatcherProvider
) : ChatRepository {
    
    override fun sendMessage(
        conversationId: String, 
        content: String
    ): Flow<ChatMessage> = flow {
        // 1. Save user message
        val userMessage = localDataSource.saveMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = content,
                status = MessageStatus.SENT
            )
        )
        emit(userMessage.toDomain())
        
        // 2. RAG augmentation (if enabled)
        val augmentedContent = if (ragPipeline != null && config.ragEnabled) {
            ragPipeline.query(content).augmentedPrompt
        } else content
        
        // 3. Prepare tools (if MCP enabled)
        val tools = if (mcpClient != null && config.mcpEnabled) {
            mcpClient.listTools().map { it.toTool() }
        } else emptyList()
        
        // 4. Build request
        val history = localDataSource.getMessages(conversationId).first()
        val request = ChatRequest(
            messages = history.map { it.toMessage() } + Message.User(augmentedContent),
            tools = tools
        )
        
        // 5. Stream response
        val assistantMessage = localDataSource.saveMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
        )
        
        val contentBuilder = StringBuilder()
        val toolCallsBuilder = mutableListOf<ToolCall>()
        
        llmClient.chat(request).collect { chunk ->
            chunk.delta?.let { contentBuilder.append(it) }
            chunk.toolCalls?.let { toolCallsBuilder.addAll(it) }
            
            emit(assistantMessage.copy(
                content = contentBuilder.toString(),
                toolCalls = toolCallsBuilder.toList().takeIf { it.isNotEmpty() }
            ).toDomain())
            
            // Handle tool calls
            if (chunk.finishReason == FinishReason.TOOL_CALLS && mcpClient != null) {
                for (toolCall in toolCallsBuilder) {
                    val result = mcpClient.callTool(toolCall.name, toolCall.arguments)
                    
                    // Save tool result
                    localDataSource.saveMessage(
                        ChatMessageEntity(
                            conversationId = conversationId,
                            role = MessageRole.TOOL,
                            content = result.toToolResult(toolCall.id).content,
                            toolCallId = toolCall.id
                        )
                    )
                    
                    // Continue conversation with tool result
                    // (recursive call or continuation logic)
                }
            }
        }
        
        // 6. Save final message
        localDataSource.updateMessage(
            assistantMessage.id,
            content = contentBuilder.toString(),
            toolCalls = toolCallsBuilder.toList(),
            status = MessageStatus.SENT
        )
        
    }.flowOn(dispatchers.io)
}

data class ChatConfig(
    val ragEnabled: Boolean = false,
    val mcpEnabled: Boolean = false,
    val systemPrompt: String? = null
)

// === UI Layer ===

class ChatViewModel(
    private val repository: ChatRepository,
    private val navigationBus: NavigationBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String = savedStateHandle["conversationId"] 
        ?: throw IllegalArgumentException("conversationId required")
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    val messages: StateFlow<List<ChatMessage>> = repository
        .getMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun updateInput(text: String) {
        _inputText.value = text
    }
    
    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isEmpty() || _isSending.value) return
        
        _inputText.value = ""
        _isSending.value = true
        
        viewModelScope.launch {
            repository.sendMessage(conversationId, content)
                .catch { /* handle error */ }
                .onCompletion { _isSending.value = false }
                .collect()
        }
    }
    
    // Cross-feature navigation (uses other feature's api module)
    fun summarizeConversation() {
        val allContent = messages.value.joinToString("\n") { it.content }
        
        // Type-safe navigation to summarizer
        SummarizerNavigation.openSummarizer(
            bus = navigationBus,
            text = allContent,
            fallback = { /* Show toast: "Summarizer not available" */ }
        )
    }
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                isSending = isSending
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            reverseLayout = true
        ) {
            items(messages.reversed(), key = { it.id }) { message ->
                ChatMessageItem(message = message)
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            MarkdownText(
                text = message.content,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            enabled = !isSending
        )
        
        Spacer(Modifier.width(8.dp))
        
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

// === Nav Graph ===

fun NavGraphBuilder.chatNavGraph(
    onNavigateBack: () -> Unit
) {
    navigation(startDestination = "chat/conversations", route = "chat") {
        composable("chat/conversations") {
            ConversationsScreen(
                onConversationClick = { id ->
                    // Internal navigation
                },
                onNewChat = { /* navigate to new */ },
                onNavigateBack = onNavigateBack
            )
        }
        
        composable(
            route = "chat/conversation/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            ChatScreen(onNavigateBack = onNavigateBack)
        }
        
        composable("chat/new") {
            NewChatScreen(onNavigateBack = onNavigateBack)
        }
    }
}

// === DI ===

val chatModule = module {
    // Local data source
    single { ChatLocalDataSource(get()) }
    
    // Repository with optional dependencies
    single<ChatRepository> {
        ChatRepositoryImpl(
            llmClient = get(),
            mcpClient = getOrNull(),    // null if MCP module not included
            ragPipeline = getOrNull(),  // null if RAG module not included
            localDataSource = get(),
            config = get(),
            dispatchers = get()
        )
    }
    
    // ViewModel
    viewModel { params ->
        ChatViewModel(
            repository = get(),
            navigationBus = get(),
            savedStateHandle = params.get()
        )
    }
    
    // Navigation handler (registered on app startup)
    factory { (navController: NavController) ->
        ChatNavigationHandler(navController).also { handler ->
            get<NavigationBus>().register(handler)
        }
    }
}
```

**Зависимости:** 
- :feature:chat:api
- :feature:summarizer:api (для cross-feature навигации)
- :core:common, :core:model, :core:ui, :core:database
- :tool:llm
- :tool:mcp (optional)
- :tool:rag (optional)

---

### :feature:summarizer:api

```kotlin
sealed interface SummarizerIntent : NavigationIntent

data class OpenSummarizerIntent(
    val text: String? = null,
    val documentUri: String? = null
) : SummarizerIntent

object SummarizerNavigation {
    fun openSummarizer(
        bus: NavigationBus,
        text: String? = null,
        documentUri: String? = null,
        fallback: (() -> Unit)? = null
    ) {
        bus.send(OpenSummarizerIntent(text, documentUri), fallback)
    }
}
```

**Зависимости:** :core:navigation

---

### :feature:summarizer:impl

```kotlin
internal class SummarizerNavigationHandler(
    private val navController: NavController
) : NavigationHandler<SummarizerIntent>(SummarizerIntent::class) {
    
    override fun handle(intent: SummarizerIntent): Boolean {
        when (intent) {
            is OpenSummarizerIntent -> {
                navController.navigate("summarizer") {
                    // Pass text/documentUri via SavedStateHandle
                }
            }
        }
        return true
    }
}

// ... ViewModel, Screen, NavGraph, DI module
```

**Зависимости:** :feature:summarizer:api, :feature:chat:api, :core:*, :tool:llm

---

## App модули

### :app:android

```kotlin
// Application
class LlmToolkitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@LlmToolkitApp)
            modules(appModules)
        }
    }
}

// Modules configuration
val appModules = listOf(
    // Core
    coreModule,
    networkModule,
    databaseModule,
    navigationModule,
    
    // Tools
    llmModule,
    mcpModule,
    ragModule,
    
    // Features
    chatModule,
    summarizerModule,
    
    // App-specific
    appConfigModule
)

val appConfigModule = module {
    single {
        LlmConfig(
            baseUrl = BuildConfig.LLM_BASE_URL,
            apiKey = BuildConfig.LLM_API_KEY,
            defaultModel = "gpt-4"
        )
    }
    
    single {
        ChatConfig(
            ragEnabled = true,
            mcpEnabled = true
        )
    }
}

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navigationBus: NavigationBus = koinInject()
    
    // Register navigation handlers
    LaunchedEffect(navController) {
        // Chat handler
        val chatHandler = ChatNavigationHandler(navController)
        navigationBus.register(chatHandler)
        
        // Summarizer handler
        val summarizerHandler = SummarizerNavigationHandler(navController)
        navigationBus.register(summarizerHandler)
        
        // Mark ready to process pending intents
        navigationBus.markReady()
    }
    
    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        chatNavGraph(onNavigateBack = { navController.popBackStack() })
        summarizerNavGraph(onNavigateBack = { navController.popBackStack() })
    }
}
```

---

### :app:chat-mobile

Минимальное приложение только с чатом (без MCP, RAG, Summarizer).

```kotlin
// Reduced modules
val chatOnlyModules = listOf(
    // Core
    coreModule,
    networkModule,
    databaseModule,
    navigationModule,
    
    // Tools (only LLM)
    llmModule,
    
    // Features (only chat)
    chatModule,
    
    // Config
    minimalConfigModule
)

val minimalConfigModule = module {
    single {
        LlmConfig(
            baseUrl = BuildConfig.LLM_BASE_URL,
            apiKey = BuildConfig.LLM_API_KEY,
            defaultModel = "gpt-3.5-turbo"
        )
    }
    
    single {
        ChatConfig(
            ragEnabled = false,  // No RAG
            mcpEnabled = false   // No MCP
        )
    }
}

@Composable
fun ChatOnlyNavHost() {
    val navController = rememberNavController()
    val navigationBus: NavigationBus = koinInject()
    
    LaunchedEffect(navController) {
        val chatHandler = ChatNavigationHandler(navController)
        navigationBus.register(chatHandler)
        navigationBus.markReady()
    }
    
    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        chatNavGraph(onNavigateBack = { /* exit app or no-op */ })
        // No summarizer — if chat tries to navigate there, fallback will trigger
    }
}
```

---

## Граф зависимостей

```
                                    ┌─────────────────┐
                                    │   :app:android  │
                                    │   :app:ios      │
                                    │   :app:desktop  │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
         ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
         │:feature:chat:impl│    │:feature:summ:impl│    │  (other :impl)   │
         └────────┬─────────┘    └────────┬─────────┘    └──────────────────┘
                  │                       │
       ┌──────────┼───────────┐          │
       │          │           │          │
       ▼          ▼           ▼          ▼
┌────────────┐ ┌─────────┐ ┌────────────────┐
│:feature:   │ │:feature:│ │   :tool:llm    │
│chat:api    │ │summ:api │ │   :tool:mcp    │
└─────┬──────┘ └────┬────┘ │   :tool:rag    │
      │             │      └───────┬────────┘
      │             │              │
      └──────┬──────┴──────────────┘
             │
             ▼
    ┌─────────────────┐
    │ :core:navigation│
    │ :core:model     │
    │ :core:network   │
    │ :core:database  │
    │ :core:ui        │
    │ :core:common    │
    └─────────────────┘
```

**Правило зависимостей:**
- `impl` → свой `api` + чужие `api` + `tools` + `core`
- `api` → только `:core:navigation`
- `tools` → только `:core:*`
- Никогда: `impl` → другой `impl`

---

## Build Logic

### Convention Plugins

```kotlin
// build-logic/convention/src/main/kotlin/KmpLibraryPlugin.kt
class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("org.jetbrains.kotlin.plugin.serialization")
        }
        
        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilations.all {
                    kotlinOptions {
                        jvmTarget = "17"
                    }
                }
            }
            
            iosX64()
            iosArm64()
            iosSimulatorArm64()
            
            jvm("desktop")
            
            sourceSets {
                val commonMain by getting {
                    dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                        implementation(libs.findLibrary("koin-core").get())
                    }
                }
                
                val commonTest by getting {
                    dependencies {
                        implementation(libs.findLibrary("kotlin-test").get())
                        implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                    }
                }
            }
        }
    }
}

// build-logic/convention/src/main/kotlin/FeatureApiPlugin.kt
class FeatureApiPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply<KmpLibraryPlugin>()
        
        // Feature API modules are minimal — only navigation dependency
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                val commonMain by getting {
                    dependencies {
                        implementation(project(":core:navigation"))
                    }
                }
            }
        }
    }
}

// build-logic/convention/src/main/kotlin/FeatureImplPlugin.kt
class FeatureImplPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply<KmpLibraryPlugin>()
        
        with(pluginManager) {
            apply("org.jetbrains.compose")
        }
        
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                val commonMain by getting {
                    dependencies {
                        // Core modules
                        implementation(project(":core:common"))
                        implementation(project(":core:model"))
                        implementation(project(":core:navigation"))
                        implementation(project(":core:ui"))
                        
                        // Compose
                        implementation(compose.runtime)
                        implementation(compose.foundation)
                        implementation(compose.material3)
                        
                        // ViewModel
                        implementation(libs.findLibrary("lifecycle-viewmodel-compose").get())
                    }
                }
            }
        }
    }
}
```

### Module Build Files

```kotlin
// :core:model/build.gradle.kts
plugins {
    id("kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
        }
    }
}

// :feature:chat:api/build.gradle.kts
plugins {
    id("feature.api")
}

// :feature:chat:impl/build.gradle.kts
plugins {
    id("feature.impl")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Own API
            implementation(project(":feature:chat:api"))
            
            // Other feature APIs (for cross-navigation)
            implementation(project(":feature:summarizer:api"))
            
            // Tools
            implementation(project(":tool:llm"))
            
            // Database
            implementation(project(":core:database"))
        }
    }
}

// :app:android/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.compose")
}

dependencies {
    // Features (impl modules)
    implementation(project(":feature:chat:impl"))
    implementation(project(":feature:summarizer:impl"))
    
    // Tools (for DI configuration)
    implementation(project(":tool:llm"))
    implementation(project(":tool:mcp"))
    implementation(project(":tool:rag"))
    
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
}
```

---

## Итоговый список модулей (~18 модулей)

| Модуль                   | Тип           | Платформы   | Описание                     |
|--------------------------|---------------|-------------|------------------------------|
| :core:common             | library       | all         | Утилиты, Result, extensions  |
| :core:model              | library       | all         | Общие доменные модели        |
| :core:navigation         | library       | all         | NavigationBus, интенты       |
| :core:network            | library       | all         | Ktor client                  |
| :core:database           | library       | all         | Room setup                   |
| :core:ui                 | library       | client      | Compose UI kit               |
| :tool:llm                | library       | all         | LLM клиент                   |
| :tool:mcp                | library       | all         | MCP клиент + сервер          |
| :tool:rag                | library       | all         | RAG pipeline                 |
| :feature:chat:api        | library       | client      | Chat навигационные контракты |
| :feature:chat:impl       | library       | client      | Chat реализация              |
| :feature:summarizer:api  | library       | client      | Summarizer контракты         |
| :feature:summarizer:impl | library       | client      | Summarizer реализация        |
| :app:android             | application   | android     | Full Android app             |
| :app:ios                 | application   | ios         | Full iOS app                 |
| :app:desktop             | application   | jvm         | Full Desktop app             |
| :app:chat-mobile         | application   | android+ios | Minimal chat app             |
| :build-logic:convention  | gradle plugin | -           | Convention plugins           |

---

## Тестирование

```kotlin
// :tool:llm/src/commonTest/
class OpenAiCompatibleClientTest {
    private val mockEngine = MockEngine { request ->
        respond(
            content = """{"choices":[{"message":{"content":"Hello!"}}]}""",
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    
    @Test
    fun `chatComplete returns response`() = runTest {
        val client = OpenAiCompatibleClient(
            HttpClient(mockEngine),
            LlmConfig(baseUrl = "http://test", defaultModel = "test")
        )
        
        val response = client.chatComplete(
            ChatRequest(messages = listOf(Message.User("Hi")))
        )
        
        assertEquals("Hello!", response.message.content)
    }
}

// :feature:chat:impl/src/commonTest/
class ChatRepositoryTest {
    private val fakeLlmClient = FakeLlmClient()
    private val fakeDataSource = FakeChatLocalDataSource()
    
    @Test
    fun `sendMessage saves and streams`() = runTest {
        val repository = ChatRepositoryImpl(
            llmClient = fakeLlmClient,
            mcpClient = null,
            ragPipeline = null,
            localDataSource = fakeDataSource,
            config = ChatConfig(),
            dispatchers = TestDispatcherProvider()
        )
        
        val messages = repository.sendMessage("conv1", "Hello").toList()
        
        assertEquals(2, messages.size)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
    }
}

// :app:android/src/androidTest/
class ChatScreenTest {
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun `send button enabled when input not empty`() {
        composeRule.setContent {
            ChatInputBar(
                text = "Hello",
                onTextChange = {},
                onSend = {},
                isSending = false
            )
        }
        
        composeRule.onNodeWithContentDescription("Send")
            .assertIsEnabled()
    }
}
```
