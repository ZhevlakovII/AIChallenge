package ru.izhxx.aichallenge.rag.docindexer.fs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.rag.docindexer.core.api.ContentReader
import ru.izhxx.aichallenge.rag.docindexer.core.api.Hasher
import ru.izhxx.aichallenge.rag.docindexer.core.api.IndexWriter
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import ru.izhxx.aichallenge.rag.docindexer.core.model.FileEntry
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.isRegularFile

/**
 * Реализация чтения контента из локальной файловой системы (только .md).
 */
class FsContentReaderJvm : ContentReader {
    override fun scanMarkdownFiles(rootDir: String): List<FileEntry> {
        val root = Paths.get(rootDir).normalize().toAbsolutePath()
        if (!Files.exists(root)) return emptyList()
        val result = mutableListOf<FileEntry>()
        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() && it.fileName.toString().endsWith(".md", ignoreCase = true) }
                .forEach { path ->
                    val rel = root.relativize(path).toString().replace(File.separatorChar, '/')
                    result.add(
                        FileEntry(
                            absolutePath = path.toAbsolutePath().toString(),
                            relativePath = rel
                        )
                    )
                }
        }
        return result.sortedBy { it.relativePath.lowercase() }
    }

    override fun read(entry: FileEntry): String {
        val bytes = Files.readAllBytes(Paths.get(entry.absolutePath))
        return String(bytes, StandardCharsets.UTF_8)
    }
}

/**
 * Реализация SHA-256 хэширования текста.
 */
class Sha256Hasher : Hasher {
    override fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Запись индекса в JSON.
 */
class JsonIndexWriter(
    private val pretty: Boolean = false
) : IndexWriter {
    private val json = Json {
        prettyPrint = pretty
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun write(index: DocumentIndex, outputPath: String) {
        val outPath: Path = Paths.get(outputPath).toAbsolutePath().normalize()
        Files.createDirectories(outPath.parent)
        val content = json.encodeToString(index)
        Files.write(outPath, content.toByteArray(StandardCharsets.UTF_8))
    }
}
