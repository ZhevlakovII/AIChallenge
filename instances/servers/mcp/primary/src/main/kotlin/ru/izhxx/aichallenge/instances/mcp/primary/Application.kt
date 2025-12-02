package ru.izhxx.aichallenge.instances.mcp.primary

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.SERVER_PORT
import ru.izhxx.aichallenge.mcp.data.model.McpToolDTO
import ru.izhxx.aichallenge.mcp.data.model.ToolsListResult
import ru.izhxx.aichallenge.mcp.data.rpc.InitializeResult
import ru.izhxx.aichallenge.mcp.data.rpc.RpcError
import ru.izhxx.aichallenge.mcp.data.rpc.RpcRequest
import ru.izhxx.aichallenge.mcp.data.rpc.RpcResponse
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.streams.asSequence

/**
 * Primary MCP-сервер.
 * WS: ws://127.0.0.1:SERVER_PORT/mcp
 *
 * В этом инстансе реализованы локальные инструменты без сети:
 * - workspace.search_in_files
 * - workspace.write_text
 * - textops.extract_todos
 * - mathops.aggregate_tasks
 *
 * (Старые инструменты сохранены для обратной совместимости.)
 */
fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = Logger("MCP-Server-Primary")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                }
            )
        }
        install(Logging)
    }

    install(WebSockets)

    routing {
        get("/") {
            call.respondText("AIChallenge MCP primary server is running")
        }

        webSocket("/mcp") {
            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }

            logger.i("Client connected to /mcp (primary)")

            val tools = buildMcpTools(json)

            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                logger.d("<= $text")

                val req = runCatching { json.decodeFromString(RpcRequest.serializer(), text) }.getOrNull()
                if (req == null) continue

                handleRpcRequest(
                    session = this,
                    req = req,
                    json = json,
                    tools = tools,
                    http = httpClient,
                    logger = logger
                )
            }

            logger.i("Client disconnected from /mcp (primary)")
        }
    }
}

private suspend fun handleRpcRequest(
    session: DefaultWebSocketServerSession,
    req: RpcRequest,
    json: Json,
    tools: List<McpToolDTO>,
    http: HttpClient,
    logger: Logger
) {
    when (req.method) {
        "initialize" -> session.handleInitialize(req, json, logger)
        "notifications/initialized" -> logger.i("notifications/initialized received")
        "tools/list" -> session.handleToolsList(req, json, tools, logger)
        "tools/call" -> session.handleToolsCall(req, json, http, logger)
        else -> session.respondError(json, req.id, -32601, "Method not found: ${req.method}", logger)
    }
}

private fun buildMcpTools(json: Json): List<McpToolDTO> {
    return listOf(
        // Базовые (были ранее)
        McpToolDTO(
            name = "health_check",
            description = "Состояние MCP сервера",
            inputSchema = null
        ),
        McpToolDTO(
            name = "get_time",
            description = "Текущее время",
            inputSchema = null
        ),
        McpToolDTO(
            name = "get_rub_usd_rate",
            description = "Текущий курс RUB→USD по @fawazahmed0/currency-api (jsDelivr)",
            inputSchema = null
        ),
        McpToolDTO(
            name = "echo",
            description = "Эхо",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Echo input",
                  "description": "Echo back the provided text.",
                  "properties": {
                    "text": {
                      "type": "string",
                      "description": "Text to echo back."
                    }
                  },
                  "required": ["text"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "sum",
            description = "Сумма двух чисел",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Sum two numbers",
                  "description": "Calculate the sum of two numbers.",
                  "properties": {
                    "a": { "type": "number", "description": "First addend." },
                    "b": { "type": "number", "description": "Second addend." }
                  },
                  "required": ["a","b"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "github.list_user_repos",
            description = "Список публичных репозиториев указанного пользователя GitHub",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "List public repositories of a GitHub user",
                  "description": "Fetch a paginated list of public repositories for the given GitHub username.",
                  "properties": {
                    "username": { "type": "string", "minLength": 1 },
                    "per_page": { "type": "integer", "minimum": 1, "maximum": 100, "default": 20 },
                    "sort": { "type": "string", "enum": ["created","updated","pushed","full_name"], "default": "updated" }
                  },
                  "required": ["username"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "github.list_my_repos",
            description = "Список репозиториев аутентифицированного пользователя GitHub (требуется GITHUB_TOKEN)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "List repositories of the authenticated user",
                  "properties": {
                    "per_page": { "type": "integer", "minimum": 1, "maximum": 100, "default": 20 },
                    "sort": { "type": "string", "enum": ["created","updated","pushed","full_name"], "default": "updated" },
                    "visibility": { "type": "string", "enum": ["all","public","private"], "default": "all" }
                  },
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),

        // Git инструменты
        McpToolDTO(
            name = "git.current_branch",
            description = "Получить текущую ветку Git репозитория",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get current Git branch",
                  "description": "Returns the name of the current branch in the Git repository.",
                  "properties": {
                    "repo_path": {
                      "type": "string",
                      "description": "Path to the Git repository (current directory if empty)",
                      "default": "."
                    }
                  },
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "git.last_commit",
            description = "Получить информацию о последнем коммите (SHA, сообщение, автор, дата)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get last Git commit",
                  "description": "Returns detailed information about the last commit in the current branch.",
                  "properties": {
                    "repo_path": {
                      "type": "string",
                      "description": "Path to the Git repository",
                      "default": "."
                    }
                  },
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "git.commit_count",
            description = "Получить количество коммитов в текущей ветке",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get Git commit count",
                  "description": "Returns the total number of commits in the current branch.",
                  "properties": {
                    "repo_path": {
                      "type": "string",
                      "description": "Path to the Git repository",
                      "default": "."
                    }
                  },
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),

        // Новые локальные инструменты (без сети)
        McpToolDTO(
            name = "workspace.search_in_files",
            description = "Поиск по файлам (regex) с опциональным контекстом и фильтром по glob",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Search in files",
                  "properties": {
                    "root_path": { "type": "string", "description": "Один корень; если относительный — будет резолвлен от корня репозитория" },
                    "roots": { "type": "array", "items": { "type": "string" }, "description": "Несколько корней; если относительные — будут резолвлены от корня репозитория", "default": [] },
                    "regex": { "type": "string", "minLength": 1 },
                    "glob": { "type": "array", "items": { "type": "string" }, "default": [] },
                    "include_content": { "type": "boolean", "default": true },
                    "context_lines": { "type": "integer", "minimum": 0, "maximum": 10, "default": 0 },
                    "max_bytes_per_file": { "type": "integer", "minimum": 1, "default": 1048576 },
                    "modified_since_iso": { "type": "string", "format": "date-time", "description": "Фильтр: изменён не раньше этого времени (ISO-8601)" },
                    "modified_until_iso": { "type": "string", "format": "date-time", "description": "Фильтр: изменён не позже этого времени (ISO-8601)" }
                  },
                  "required": ["regex"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "textops.extract_todos",
            description = "Преобразование найденных строк в структурированные задачи (TODO/FIXME)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Extract TODO items",
                  "properties": {
                    "items": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "file": { "type": "string" },
                          "line": { "type": "integer" },
                          "text": { "type": "string" }
                        },
                        "required": ["file","line","text"],
                        "additionalProperties": false
                      }
                    },
                    "patterns": {
                      "type": "array",
                      "items": { "type": "string" },
                      "default": ["TODO","FIXME"]
                    }
                  },
                  "required": ["items"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "mathops.aggregate_tasks",
            description = "Агрегация задач по приоритетам и тегам",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Aggregate tasks",
                  "properties": {
                    "tasks": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "title": { "type": "string" },
                          "priority": { "type": "string" },
                          "tags": {
                            "type": "array",
                            "items": { "type": "string" },
                            "default": []
                          }
                        },
                        "required": ["title"],
                        "additionalProperties": true
                      }
                    }
                  },
                  "required": ["tasks"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),

        // PR инструменты
        McpToolDTO(
            name = "pr.info",
            description = "Получить метаинформацию о Pull Request (автор, описание, статус, файлы). Требуется GITHUB_TOKEN.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get Pull Request info",
                  "description": "Retrieve metadata about a GitHub Pull Request including author, description, status, and files.",
                  "properties": {
                    "owner": {
                      "type": "string",
                      "description": "Repository owner (username or organization)"
                    },
                    "repo": {
                      "type": "string",
                      "description": "Repository name"
                    },
                    "pr_number": {
                      "type": "integer",
                      "description": "Pull request number"
                    }
                  },
                  "required": ["owner", "repo", "pr_number"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "pr.diff",
            description = "Получить unified diff Pull Request. Требуется GITHUB_TOKEN.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get Pull Request diff",
                  "description": "Retrieve the unified diff of a GitHub Pull Request.",
                  "properties": {
                    "owner": {
                      "type": "string",
                      "description": "Repository owner (username or organization)"
                    },
                    "repo": {
                      "type": "string",
                      "description": "Repository name"
                    },
                    "pr_number": {
                      "type": "integer",
                      "description": "Pull request number"
                    }
                  },
                  "required": ["owner", "repo", "pr_number"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "pr.files",
            description = "Получить список измененных файлов в Pull Request. Требуется GITHUB_TOKEN.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get Pull Request files",
                  "description": "Retrieve the list of changed files in a GitHub Pull Request.",
                  "properties": {
                    "owner": {
                      "type": "string",
                      "description": "Repository owner (username or organization)"
                    },
                    "repo": {
                      "type": "string",
                      "description": "Repository name"
                    },
                    "pr_number": {
                      "type": "integer",
                      "description": "Pull request number"
                    }
                  },
                  "required": ["owner", "repo", "pr_number"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "pr.file_content",
            description = "Получить содержимое конкретного файла из ветки Pull Request. Требуется GITHUB_TOKEN.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get file content from PR branch",
                  "description": "Retrieve the content of a specific file from a Pull Request branch.",
                  "properties": {
                    "owner": {
                      "type": "string",
                      "description": "Repository owner (username or organization)"
                    },
                    "repo": {
                      "type": "string",
                      "description": "Repository name"
                    },
                    "pr_number": {
                      "type": "integer",
                      "description": "Pull request number"
                    },
                    "file_path": {
                      "type": "string",
                      "description": "Path to the file in the repository"
                    }
                  },
                  "required": ["owner", "repo", "pr_number", "file_path"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        )
    )
}

private suspend fun DefaultWebSocketServerSession.handleInitialize(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(
        InitializeResult(
            serverInfo = buildJsonObject {
                put("name", "AIChallenge-MCP-Primary")
                put("version", "1.1.0")
            },
            capabilities = buildJsonObject { }
        )
    )
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleToolsList(
    req: RpcRequest,
    json: Json,
    tools: List<McpToolDTO>,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(ToolsListResult(tools = tools))
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleToolsCall(
    req: RpcRequest,
    json: Json,
    http: HttpClient,
    logger: Logger
) {
    val params = req.params?.jsonObject
    val toolName = params?.get("name")?.jsonPrimitive?.content
    val args = params?.get("arguments")?.jsonObject

    if (toolName == null) {
        respondError(json, req.id, -32602, "Invalid params: 'name' is required", logger)
        return
    }

    when (toolName) {
        // Существующие
        "health_check" -> handleHealthCheck(req, json, logger)
        "get_time" -> handleGetTime(req, json, logger)
        "get_rub_usd_rate" -> handleGetRubUsdRate(req, json, http, logger)
        "echo" -> handleEcho(req, json, args, logger)
        "sum" -> handleSum(req, json, args, logger)
        "github.list_user_repos" -> handleGithubListUserRepos(req, json, args, http, logger)
        "github.list_my_repos" -> handleGithubListMyRepos(req, json, args, http, logger)

        // Git инструменты
        "git.current_branch" -> handleGitCurrentBranch(req, json, args, logger)
        "git.last_commit" -> handleGitLastCommit(req, json, args, logger)
        "git.commit_count" -> handleGitCommitCount(req, json, args, logger)

        // Новые локальные (без сети)
        "workspace.search_in_files" -> handleWorkspaceSearchInFiles(req, json, args, logger)
        "textops.extract_todos" -> handleTextopsExtractTodos(req, json, args, logger)
        "mathops.aggregate_tasks" -> handleMathopsAggregateTasks(req, json, args, logger)

        // PR инструменты
        "pr.info" -> handlePrInfo(req, json, args, http, logger)
        "pr.diff" -> handlePrDiff(req, json, args, http, logger)
        "pr.files" -> handlePrFiles(req, json, args, http, logger)
        "pr.file_content" -> handlePrFileContent(req, json, args, http, logger)

        else -> respondError(json, req.id, -32601, "Unknown tool: $toolName", logger)
    }
}

// ======= СТАРЫЕ ОБРАБОТЧИКИ =======

private suspend fun DefaultWebSocketServerSession.handleHealthCheck(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val resultEl = buildJsonObject { put("status", JsonPrimitive("ok")) }
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleGetTime(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val now = Instant.now()
    val iso = DateTimeFormatter.ISO_INSTANT.format(now)
    val resultEl = buildJsonObject { put("iso", JsonPrimitive(iso)) }
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleGetRubUsdRate(
    req: RpcRequest,
    json: Json,
    http: HttpClient,
    logger: Logger
) {
    val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/rub.json"
    val response: HttpResponse = http.get(url) { header("Accept", "application/json") }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val rootEl = runCatching { json.parseToJsonElement(body) }.getOrNull()
        val rubObj = rootEl?.jsonObject?.get("rub")?.jsonObject
        val usd = rubObj?.get("usd")?.jsonPrimitive?.content?.toDoubleOrNull()

        if (usd != null) {
            val fetchedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val resultEl = buildJsonObject {
                put("base", JsonPrimitive("RUB"))
                put("symbol", JsonPrimitive("USD"))
                put("rate", JsonPrimitive(usd))
                put("fetchedAt", JsonPrimitive(fetchedAt))
                put("source", JsonPrimitive(url))
            }
            respondResult(json, req.id, resultEl, logger)
        } else {
            respondError(json, req.id, -32001, "Missing rub.usd rate in API response", logger)
        }
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "Currency API error (${response.status.value}): $body", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handleEcho(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val text = args?.get("text")?.jsonPrimitive?.content
    if (text == null) {
        respondError(json, req.id, -32602, "Invalid params: 'text' is required", logger)
        return
    }
    val resultEl = buildJsonObject {
        put("text", JsonPrimitive(text))
        put("length", JsonPrimitive(text.length))
    }
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleSum(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val a = args?.get("a")?.jsonPrimitive?.content?.toDoubleOrNull()
    val b = args?.get("b")?.jsonPrimitive?.content?.toDoubleOrNull()
    if (a == null || b == null) {
        respondError(json, req.id, -32602, "Invalid params: 'a' and 'b' numbers are required", logger)
        return
    }
    val resultEl = buildJsonObject { put("result", JsonPrimitive(a + b)) }
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleGithubListUserRepos(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val username = args?.get("username")?.jsonPrimitive?.content
    val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: 20
    val sort = args?.get("sort")?.jsonPrimitive?.content ?: "updated"

    if (username.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'username' is required", logger)
        return
    }

    val url = "https://api.github.com/users/${username}/repos"
    val response: HttpResponse = http.get(url) {
        parameter("per_page", perPage)
        parameter("sort", sort)
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val arrayEl = runCatching { json.parseToJsonElement(body) }.getOrNull()
        val resultEl = buildJsonObject { put("items", arrayEl ?: json.parseToJsonElement("[]")) }
        respondResult(json, req.id, resultEl, logger)
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handleGithubListMyRepos(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: 20
    val sort = args?.get("sort")?.jsonPrimitive?.content ?: "updated"
    val visibility = args?.get("visibility")?.jsonPrimitive?.content ?: "all"
    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
    if (token.isEmpty()) {
        respondError(json, req.id, -32000, "GitHub token not configured on server (GITHUB_TOKEN)", logger)
        return
    }

    val url = "https://api.github.com/user/repos"
    val response: HttpResponse = http.get(url) {
        parameter("per_page", perPage)
        parameter("sort", sort)
        parameter("visibility", visibility)
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val arrayEl = runCatching { json.parseToJsonElement(body) }.getOrNull()
        val resultEl = buildJsonObject { put("items", arrayEl ?: json.parseToJsonElement("[]")) }
        respondResult(json, req.id, resultEl, logger)
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

// ======= GIT ИНСТРУМЕНТЫ =======

private suspend fun DefaultWebSocketServerSession.handleGitCurrentBranch(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val repoPath = args?.get("repo_path")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "."

    try {
        val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .directory(Paths.get(repoPath).toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        if (exitCode == 0 && output.isNotEmpty()) {
            val resultEl = buildJsonObject {
                put("branch", JsonPrimitive(output))
                put("repo_path", JsonPrimitive(repoPath))
            }
            respondResult(json, req.id, resultEl, logger)
        } else {
            respondError(json, req.id, -32001, "Failed to get Git branch: $output", logger)
        }
    } catch (e: Exception) {
        respondError(json, req.id, -32000, "Git error: ${e.message}", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handleGitLastCommit(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val repoPath = args?.get("repo_path")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "."

    try {
        // Получить хеш коммита
        val hashProcess = ProcessBuilder("git", "rev-parse", "HEAD")
            .directory(Paths.get(repoPath).toFile())
            .redirectErrorStream(true)
            .start()

        val hash = hashProcess.inputStream.bufferedReader().readText().trim()
        if (hashProcess.waitFor() != 0) throw Exception("Failed to get commit hash")

        // Получить сообщение коммита
        val msgProcess = ProcessBuilder("git", "log", "-1", "--pretty=%B")
            .directory(Paths.get(repoPath).toFile())
            .redirectErrorStream(true)
            .start()

        val message = msgProcess.inputStream.bufferedReader().readText().trim()
        if (msgProcess.waitFor() != 0) throw Exception("Failed to get commit message")

        // Получить автора и дату
        val authorProcess = ProcessBuilder("git", "log", "-1", "--pretty=%an|%ae|%aI")
            .directory(Paths.get(repoPath).toFile())
            .redirectErrorStream(true)
            .start()

        val authorInfo = authorProcess.inputStream.bufferedReader().readText().trim()
        if (authorProcess.waitFor() != 0) throw Exception("Failed to get author info")

        val (author, email, date) = authorInfo.split("|").let { parts ->
            Triple(parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "")
        }

        val resultEl = buildJsonObject {
            put("hash", JsonPrimitive(hash.take(7))) // Short hash
            put("hash_full", JsonPrimitive(hash))
            put("message", JsonPrimitive(message))
            put("author", JsonPrimitive(author))
            put("email", JsonPrimitive(email))
            put("date", JsonPrimitive(date))
            put("repo_path", JsonPrimitive(repoPath))
        }
        respondResult(json, req.id, resultEl, logger)
    } catch (e: Exception) {
        respondError(json, req.id, -32000, "Git error: ${e.message}", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handleGitCommitCount(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val repoPath = args?.get("repo_path")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "."

    try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(Paths.get(repoPath).toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        if (exitCode == 0 && output.isNotEmpty()) {
            val count = output.toIntOrNull() ?: 0
            val resultEl = buildJsonObject {
                put("count", JsonPrimitive(count))
                put("repo_path", JsonPrimitive(repoPath))
            }
            respondResult(json, req.id, resultEl, logger)
        } else {
            respondError(json, req.id, -32001, "Failed to get commit count: $output", logger)
        }
    } catch (e: Exception) {
        respondError(json, req.id, -32000, "Git error: ${e.message}", logger)
    }
}

// ======= НОВЫЕ ЛОКАЛЬНЫЕ ОБРАБОТЧИКИ =======

private suspend fun DefaultWebSocketServerSession.handleWorkspaceSearchInFiles(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val regexStr = args?.get("regex")?.jsonPrimitive?.content
    if (regexStr.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'regex' is required", logger)
        return
    }
    val includeContent = args["include_content"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
    val contextLines = args["context_lines"]?.jsonPrimitive?.intOrNull ?: 0
    val maxBytes = args["max_bytes_per_file"]?.jsonPrimitive?.intOrNull ?: 1_048_576

    val globArr = args["glob"]?.jsonArray
    val globs = globArr?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

    val rootsArr = args["roots"]?.jsonArray
    val rootsList = rootsArr?.mapNotNull { it.jsonPrimitive.contentOrNull }?.filter { it.isNotBlank() } ?: emptyList()
    val singleRootPathStr = args?.get("root_path")?.jsonPrimitive?.content

    val modifiedSinceIso = args["modified_since_iso"]?.jsonPrimitive?.content
    val modifiedUntilIso = args["modified_until_iso"]?.jsonPrimitive?.content
    val modifiedSince = modifiedSinceIso?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
    val modifiedUntil = modifiedUntilIso?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }

    fun findRepoRoot(start: Path = Paths.get("").toAbsolutePath().normalize()): Path? {
        var cur = start
        repeat(20) {
            val dotGit = cur.resolve(".git")
            val gradlew = cur.resolve("gradlew")
            val settings = cur.resolve("settings.gradle.kts")
            if (Files.isDirectory(dotGit) || Files.exists(gradlew) || Files.exists(settings)) return cur
            cur = cur.parent ?: return null
        }
        return null
    }

    fun resolveRoot(input: String): Path? {
        val p = Paths.get(input).normalize()
        if (Files.isDirectory(p)) return p
        val repo = findRepoRoot()
        if (repo != null) {
            val candidate = repo.resolve(input).normalize()
            if (Files.isDirectory(candidate)) return candidate
        }
        return null
    }

    val candidateRoots: List<Path> = when {
        rootsList.isNotEmpty() -> rootsList.mapNotNull { resolveRoot(it) }
        !singleRootPathStr.isNullOrBlank() -> listOfNotNull(resolveRoot(singleRootPathStr))
        else -> emptyList()
    }

    if (candidateRoots.isEmpty()) {
        respondError(json, req.id, -32602, "Invalid params: provide 'root_path' or 'roots' that resolve to existing directories", logger)
        return
    }

    val compiled = Pattern.compile(regexStr)
    val matchers: List<PathMatcher> = globs.map { pattern ->
        FileSystems.getDefault().getPathMatcher("glob:$pattern")
    }

    fun filePassesGlob(root: Path, path: Path): Boolean {
        if (matchers.isEmpty()) return true
        val rel = root.relativize(path).toString().replace('\\', '/')
        return matchers.any { m ->
            m.matches(Paths.get(rel)) || m.matches(Paths.get("./$rel"))
        }
    }

    fun filePassesTimeFilter(path: Path): Boolean {
        if (modifiedSince == null && modifiedUntil == null) return true
        val lm = runCatching { Files.getLastModifiedTime(path).toInstant() }.getOrNull() ?: return true
        if (modifiedSince != null && lm.isBefore(modifiedSince)) return false
        if (modifiedUntil != null && lm.isAfter(modifiedUntil)) return false
        return true
    }

    val matchesJson = buildJsonArray {
        candidateRoots.forEach { root ->
            Files.walk(root).use { stream ->
                stream.asSequence()
                    .filter { Files.isRegularFile(it) }
                    .filter { !it.toString().contains("${FileSystems.getDefault().separator}.git${FileSystems.getDefault().separator}") }
                    .filter { path -> filePassesGlob(root, path) }
                    .filter { path -> filePassesTimeFilter(path) }
                    .forEach { path ->
                        val size = runCatching { Files.size(path) }.getOrDefault(Long.MAX_VALUE)
                        if (size > maxBytes) return@forEach
                        val content = runCatching { Files.readAllLines(path, StandardCharsets.UTF_8) }.getOrNull() ?: return@forEach
                        content.forEachIndexed { idx, line ->
                            val matcher = compiled.matcher(line)
                            if (matcher.find()) {
                                val lineNum = idx + 1
                                val before =
                                    if (includeContent && contextLines > 0) content.subList(maxOf(0, idx - contextLines), idx) else emptyList()
                                val after =
                                    if (includeContent && contextLines > 0) content.subList(minOf(content.size, idx + 1), minOf(content.size, idx + 1 + contextLines)) else emptyList()

                                add(
                                    buildJsonObject {
                                        put("file", JsonPrimitive(path.toString()))
                                        put("line", JsonPrimitive(lineNum))
                                        put("text", JsonPrimitive(line))
                                        if (includeContent && contextLines > 0) {
                                            put("before", json.encodeToJsonElement(before))
                                            put("after", json.encodeToJsonElement(after))
                                        }
                                    }
                                )
                            }
                        }
                    }
            }
        }
    }

    val resultEl = buildJsonObject {
        put("matches", matchesJson)
    }
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleTextopsExtractTodos(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val items = args?.get("items")?.jsonArray
    if (items == null) {
        respondError(json, req.id, -32602, "Invalid params: 'items' array is required", logger)
        return
    }
    val patterns = args["patterns"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: listOf("TODO", "FIXME")
    val patternRegex = Pattern.compile(patterns.joinToString("|") { Pattern.quote(it) }, Pattern.CASE_INSENSITIVE)

    data class Task(val title: String, val priority: String?, val tags: List<String>, val sourceFile: String, val line: Int, val dueDate: String?)

    fun inferPriority(text: String): String? {
        val t = text.lowercase()
        return when {
            Regex("""\b(p0|urgent|asap)\b""").containsMatchIn(t) -> "high"
            Regex("""\b(p1|soon|important)\b""").containsMatchIn(t) -> "medium"
            Regex("""\b(p2|later|low)\b""").containsMatchIn(t) -> "low"
            else -> null
        }
    }

    fun inferTags(text: String): List<String> {
        val tags = mutableListOf<String>()
        Regex("""\[(bug|refactor|doc|feature|test|perf)\]""", RegexOption.IGNORE_CASE).findAll(text).forEach {
            tags.add(it.groupValues[1].lowercase())
        }
        if (Regex("""\bfixme\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) tags.add("fixme")
        if (Regex("""\btodo\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) tags.add("todo")
        return tags.distinct()
    }

    val tasks = buildJsonArray {
        items.forEach { el ->
            val obj = el.jsonObject
            val file = obj["file"]?.jsonPrimitive?.content ?: return@forEach
            val line = obj["line"]?.jsonPrimitive?.intOrNull ?: 0
            val text = obj["text"]?.jsonPrimitive?.content ?: return@forEach

            if (!patternRegex.matcher(text).find()) return@forEach

            val title = text.replace(Regex("""^\s*(//+|#|-|\*)\s*"""), "").trim()
            val priority = inferPriority(text)
            val tags = inferTags(text)
            add(
                buildJsonObject {
                    put("title", JsonPrimitive(title))
                    if (priority != null) put("priority", JsonPrimitive(priority))
                    put("tags", json.encodeToJsonElement(tags))
                    put("source_file", JsonPrimitive(file))
                    put("line", JsonPrimitive(line))
                }
            )
        }
    }

    val resultEl = buildJsonObject {
        put("tasks", tasks)
    }
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleMathopsAggregateTasks(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val tasks = args?.get("tasks")?.jsonArray
    if (tasks == null) {
        respondError(json, req.id, -32602, "Invalid params: 'tasks' array is required", logger)
        return
    }

    var all = 0
    var high = 0
    var medium = 0
    var low = 0
    val byTag = mutableMapOf<String, Int>()

    tasks.forEach { el ->
        all++
        val obj = el.jsonObject
        when (obj["priority"]?.jsonPrimitive?.content?.lowercase()) {
            "high" -> high++
            "medium" -> medium++
            "low" -> low++
        }
        val tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        tags.forEach { tag -> byTag[tag] = (byTag[tag] ?: 0) + 1 }
    }

    fun pct(x: Int): Double = if (all == 0) 0.0 else (x.toDouble() / all.toDouble()) * 100.0

    val resultEl = buildJsonObject {
        put("totals", buildJsonObject {
            put("all", JsonPrimitive(all))
            put("high", JsonPrimitive(high))
            put("medium", JsonPrimitive(medium))
            put("low", JsonPrimitive(low))
        })
        put("by_tag", json.encodeToJsonElement(byTag))
        put("distribution", buildJsonObject {
            put("priority_pct", buildJsonObject {
                put("high", JsonPrimitive(pct(high)))
                put("medium", JsonPrimitive(pct(medium)))
                put("low", JsonPrimitive(pct(low)))
            })
        })
    }
    respondResult(json, req.id, resultEl, logger)
}

// ======= PR ИНСТРУМЕНТЫ =======

private suspend fun DefaultWebSocketServerSession.handlePrInfo(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val owner = args?.get("owner")?.jsonPrimitive?.content
    val repo = args?.get("repo")?.jsonPrimitive?.content
    val prNumber = args?.get("pr_number")?.jsonPrimitive?.intOrNull

    if (owner.isNullOrBlank() || repo.isNullOrBlank() || prNumber == null) {
        respondError(json, req.id, -32602, "Invalid params: 'owner', 'repo', and 'pr_number' are required", logger)
        return
    }

    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
    if (token.isEmpty()) {
        respondError(json, req.id, -32000, "GitHub token not configured on server (GITHUB_TOKEN)", logger)
        return
    }

    val url = "https://api.github.com/repos/$owner/$repo/pulls/$prNumber"
    val response: HttpResponse = http.get(url) {
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val prData = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (prData != null) {
            val resultEl = buildJsonObject {
                put("number", prData["number"] ?: JsonPrimitive(prNumber))
                put("title", prData["title"] ?: JsonPrimitive(""))
                put("state", prData["state"] ?: JsonPrimitive(""))
                put("user", prData["user"] ?: buildJsonObject {})
                put("body", prData["body"] ?: JsonPrimitive(""))
                put("created_at", prData["created_at"] ?: JsonPrimitive(""))
                put("updated_at", prData["updated_at"] ?: JsonPrimitive(""))
                put("merged_at", prData["merged_at"] ?: JsonPrimitive(null))
                put("head", prData["head"] ?: buildJsonObject {})
                put("base", prData["base"] ?: buildJsonObject {})
                put("mergeable", prData["mergeable"] ?: JsonPrimitive(null))
                put("mergeable_state", prData["mergeable_state"] ?: JsonPrimitive(""))
                put("changed_files", prData["changed_files"] ?: JsonPrimitive(0))
                put("additions", prData["additions"] ?: JsonPrimitive(0))
                put("deletions", prData["deletions"] ?: JsonPrimitive(0))
                put("commits", prData["commits"] ?: JsonPrimitive(0))
            }
            respondResult(json, req.id, resultEl, logger)
        } else {
            respondError(json, req.id, -32001, "Failed to parse PR data", logger)
        }
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handlePrDiff(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val owner = args?.get("owner")?.jsonPrimitive?.content
    val repo = args?.get("repo")?.jsonPrimitive?.content
    val prNumber = args?.get("pr_number")?.jsonPrimitive?.intOrNull

    if (owner.isNullOrBlank() || repo.isNullOrBlank() || prNumber == null) {
        respondError(json, req.id, -32602, "Invalid params: 'owner', 'repo', and 'pr_number' are required", logger)
        return
    }

    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
    if (token.isEmpty()) {
        respondError(json, req.id, -32000, "GitHub token not configured on server (GITHUB_TOKEN)", logger)
        return
    }

    val url = "https://api.github.com/repos/$owner/$repo/pulls/$prNumber"
    val response: HttpResponse = http.get(url) {
        header("Accept", "application/vnd.github.v3.diff")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (response.status.isSuccess()) {
        val diff = response.bodyAsText()
        val resultEl = buildJsonObject {
            put("diff", JsonPrimitive(diff))
        }
        respondResult(json, req.id, resultEl, logger)
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handlePrFiles(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val owner = args?.get("owner")?.jsonPrimitive?.content
    val repo = args?.get("repo")?.jsonPrimitive?.content
    val prNumber = args?.get("pr_number")?.jsonPrimitive?.intOrNull

    if (owner.isNullOrBlank() || repo.isNullOrBlank() || prNumber == null) {
        respondError(json, req.id, -32602, "Invalid params: 'owner', 'repo', and 'pr_number' are required", logger)
        return
    }

    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
    if (token.isEmpty()) {
        respondError(json, req.id, -32000, "GitHub token not configured on server (GITHUB_TOKEN)", logger)
        return
    }

    val url = "https://api.github.com/repos/$owner/$repo/pulls/$prNumber/files"
    val response: HttpResponse = http.get(url) {
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val filesArray = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull()
        if (filesArray != null) {
            val resultEl = buildJsonObject {
                put("files", filesArray)
            }
            respondResult(json, req.id, resultEl, logger)
        } else {
            respondError(json, req.id, -32001, "Failed to parse files data", logger)
        }
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handlePrFileContent(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val owner = args?.get("owner")?.jsonPrimitive?.content
    val repo = args?.get("repo")?.jsonPrimitive?.content
    val prNumber = args?.get("pr_number")?.jsonPrimitive?.intOrNull
    val filePath = args?.get("file_path")?.jsonPrimitive?.content

    if (owner.isNullOrBlank() || repo.isNullOrBlank() || prNumber == null || filePath.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'owner', 'repo', 'pr_number', and 'file_path' are required", logger)
        return
    }

    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
    if (token.isEmpty()) {
        respondError(json, req.id, -32000, "GitHub token not configured on server (GITHUB_TOKEN)", logger)
        return
    }

    // Step 1: Get PR info to extract the head ref
    val prUrl = "https://api.github.com/repos/$owner/$repo/pulls/$prNumber"
    val prResponse: HttpResponse = http.get(prUrl) {
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (!prResponse.status.isSuccess()) {
        val body = runCatching { prResponse.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, prResponse.status.value, "GitHub API error getting PR info (${prResponse.status.value}): $body", logger)
        return
    }

    val prData = runCatching { json.parseToJsonElement(prResponse.bodyAsText()).jsonObject }.getOrNull()
    val headRef = prData?.get("head")?.jsonObject?.get("ref")?.jsonPrimitive?.content

    if (headRef.isNullOrBlank()) {
        respondError(json, req.id, -32001, "Failed to extract head ref from PR", logger)
        return
    }

    // Step 2: Get file content using the ref
    val contentUrl = "https://api.github.com/repos/$owner/$repo/contents/$filePath"
    val contentResponse: HttpResponse = http.get(contentUrl) {
        parameter("ref", headRef)
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (contentResponse.status.isSuccess()) {
        val body = contentResponse.bodyAsText()
        val contentData = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (contentData != null) {
            val resultEl = buildJsonObject {
                put("name", contentData["name"] ?: JsonPrimitive(""))
                put("path", contentData["path"] ?: JsonPrimitive(filePath))
                put("sha", contentData["sha"] ?: JsonPrimitive(""))
                put("size", contentData["size"] ?: JsonPrimitive(0))
                put("content", contentData["content"] ?: JsonPrimitive(""))
                put("encoding", contentData["encoding"] ?: JsonPrimitive(""))
                put("ref", JsonPrimitive(headRef))
            }
            respondResult(json, req.id, resultEl, logger)
        } else {
            respondError(json, req.id, -32001, "Failed to parse file content data", logger)
        }
    } else {
        val body = runCatching { contentResponse.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, contentResponse.status.value, "GitHub API error getting file content (${contentResponse.status.value}): $body", logger)
    }
}

// ======= ОБЩИЕ УТИЛИТЫ ОТВЕТА =======

private suspend fun DefaultWebSocketServerSession.respondResult(
    json: Json,
    id: Int?,
    result: JsonElement,
    logger: Logger
) {
    val resp = RpcResponse(id = id, result = result)
    val out = json.encodeToString(resp)
    logger.d("=> $out")
    send(Frame.Text(out))
}

private suspend fun DefaultWebSocketServerSession.respondError(
    json: Json,
    id: Int?,
    code: Int,
    message: String,
    logger: Logger
) {
    val err = RpcError(code = code, message = message)
    val resp = RpcResponse(id = id, error = err)
    val out = json.encodeToString(resp)
    logger.d("=> $out")
    send(Frame.Text(out))
}
