package ru.izhxx.aichallenge.rag.docindexer.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.rag.docindexer.core.impl.CharOverlapChunker
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildParams
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildRequest
import ru.izhxx.aichallenge.rag.docindexer.core.model.ModelConfig
import ru.izhxx.aichallenge.rag.docindexer.core.pipeline.IndexBuilder
import ru.izhxx.aichallenge.rag.docindexer.core.pipeline.computeCharWindow
import ru.izhxx.aichallenge.rag.docindexer.fs.FsContentReaderJvm
import ru.izhxx.aichallenge.rag.docindexer.fs.JsonIndexWriter
import ru.izhxx.aichallenge.rag.docindexer.fs.Sha256Hasher
import ru.izhxx.aichallenge.rag.docindexer.ollama.OllamaEmbedder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * CLI для построения локального JSON-индекса .md документов с эмбеддингами из Ollama.
 *
 * Пример:
 *   doc-indexer build \
 *     --input-dir "./docs" \
 *     --out "./rag-data/index.json" \
 *     --target-tokens 400 \
 *     --overlap-tokens 80 \
 *     --chars-per-token 3.0 \
 *     --ollama-url "http://localhost:11434" \
 *     --model "mxbai-embed-large" \
 *     --concurrency 4
 */
fun main(vararg args: String) = runBlocking {
    val cfg = try {
        parseArgs(args.toList())
    } catch (t: Throwable) {
        System.err.println("Ошибка парсинга аргументов: ${t.message}")
        printUsage()
        return@runBlocking
    }

    val (maxChars, overlapChars) = computeCharWindow(cfg.targetTokens, cfg.overlapTokens, cfg.charsPerToken)

    val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
        install(Logging) {
            level = if (cfg.verbose) LogLevel.BODY else LogLevel.NONE
        }
    }

    val reader = FsContentReaderJvm()
    val chunker = CharOverlapChunker()
    val embedder = OllamaEmbedder(
        http = http,
        baseUrl = cfg.ollamaUrl,
        model = cfg.model,
        retries = cfg.retries,
        initialBackoffMs = cfg.initialBackoffMs
    )
    val hasher = Sha256Hasher()

    val root = findProjectRoot()
    val inputAbs = resolveAgainstRoot(root, cfg.inputDir).toString()
    val outAbs = resolveAgainstRoot(root, cfg.out).toString()

    val params = BuildParams(
        targetTokens = cfg.targetTokens,
        overlapTokens = cfg.overlapTokens,
        charsPerToken = cfg.charsPerToken,
        maxChars = maxChars,
        overlapChars = overlapChars,
        concurrency = cfg.concurrency
    )
    val request = BuildRequest(
        inputDir = inputAbs,
        params = params,
        model = ModelConfig(
            name = cfg.model,
            endpoint = cfg.ollamaUrl
        )
    )

    println("doc-indexer: start build")
    println("- inputDir      = ${cfg.inputDir} -> ${inputAbs}")
    println("- out           = ${cfg.out} -> ${outAbs}")
    println("- model         = ${cfg.model}")
    println("- ollamaUrl     = ${cfg.ollamaUrl}")
    println("- targetTokens  = ${cfg.targetTokens}  (maxChars=$maxChars)")
    println("- overlapTokens = ${cfg.overlapTokens} (overlapChars=$overlapChars)")
    println("- concurrency   = ${cfg.concurrency}")

    val index = IndexBuilder(
        reader = reader,
        chunker = chunker,
        embedder = embedder,
        hasher = hasher
    ).build(request)

    JsonIndexWriter(pretty = false).write(index, outAbs)

    println("doc-indexer: done")
    println("- docs   = ${index.stats.docs}")
    println("- chunks = ${index.stats.chunks}")
    println("- avgLen = ${"%.1f".format(index.stats.avgChunkLen)}")
    println("- tookMs = ${index.stats.elapsedMs}")
    http.close()
}

private data class CliConfig(
    val command: String,
    val inputDir: String,
    val out: String,
    val targetTokens: Int = 400,
    val overlapTokens: Int = 80,
    val charsPerToken: Double = 3.0,
    val concurrency: Int = 4,
    val ollamaUrl: String = "http://localhost:11434",
    val model: String = "mxbai-embed-large",
    val retries: Int = 3,
    val initialBackoffMs: Long = 250,
    val verbose: Boolean = false
)

private fun parseArgs(args: List<String>): CliConfig {
    if (args.isEmpty()) {
        throw IllegalArgumentException("Не указан subcommand (например, build)")
    }
    val command = args.first()
    if (command != "build") {
        throw IllegalArgumentException("Поддерживается только subcommand: build")
    }

    val map = mutableMapOf<String, String>()
    var i = 1
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) {
            val eq = a.indexOf('=')
            if (eq != -1) {
                val key = a.substring(2, eq)
                val value = a.substring(eq + 1)
                map[key] = value
                i++
            } else {
                // формат: --key value
                val key = a.substring(2)
                val value = args.getOrNull(i + 1)
                    ?: throw IllegalArgumentException("Ожидалось значение для флага $a")
                map[key] = value
                i += 2
            }
        } else {
            // позиционные не поддерживаем
            i++
        }
    }

    val inputDir = map["input-dir"] ?: throw IllegalArgumentException("--input-dir обязателен")
    val out = map["out"] ?: throw IllegalArgumentException("--out обязателен")

    val targetTokens = map["target-tokens"]?.toIntOrNull() ?: 400
    val overlapTokens = map["overlap-tokens"]?.toIntOrNull() ?: 80
    val charsPerToken = map["chars-per-token"]?.toDoubleOrNull() ?: 3.0
    val concurrency = map["concurrency"]?.toIntOrNull() ?: 4
    val ollamaUrl = map["ollama-url"] ?: "http://localhost:11434"
    val model = map["model"] ?: "mxbai-embed-large"
    val retries = map["retries"]?.toIntOrNull() ?: 3
    val initialBackoffMs = map["initial-backoff-ms"]?.toLongOrNull() ?: 250
    val verbose = map["verbose"]?.toBooleanStrictOrNull() ?: false

    return CliConfig(
        command = command,
        inputDir = inputDir,
        out = out,
        targetTokens = targetTokens,
        overlapTokens = overlapTokens,
        charsPerToken = charsPerToken,
        concurrency = concurrency,
        ollamaUrl = ollamaUrl,
        model = model,
        retries = retries,
        initialBackoffMs = initialBackoffMs,
        verbose = verbose
    )
}

private fun findProjectRoot(): Path {
    val start = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
    var cur = start
    while (true) {
        if (Files.exists(cur.resolve("settings.gradle.kts")) || Files.exists(cur.resolve(".git"))) {
            return cur
        }
        val parent = cur.parent ?: return start
        cur = parent
    }
}

private fun resolveAgainstRoot(root: Path, path: String): Path {
    val p = Paths.get(path)
    return if (p.isAbsolute) p.normalize() else root.resolve(path).normalize()
}

private fun printUsage() {
    println(
        """
        doc-indexer build --input-dir <DIR> --out <FILE> [options]
        
        Options:
          --target-tokens <Int>       (default 400)
          --overlap-tokens <Int>      (default 80)
          --chars-per-token <Double>  (default 3.0 for RU)
          --concurrency <Int>         (default 4)
          --ollama-url <String>       (default http://localhost:11434)
          --model <String>            (default mxbai-embed-large)
          --retries <Int>             (default 3)
          --initial-backoff-ms <Long> (default 250)
          --verbose <true|false>      (default false)
        """.trimIndent()
    )
}
