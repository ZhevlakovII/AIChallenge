package ru.izhxx.aichallenge.data.rag

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

actual fun readFileTextCompat(path: String): String? {
    return try {
        val p = Paths.get(path)
        if (!Files.exists(p) || !Files.isRegularFile(p)) return null
        val bytes = Files.readAllBytes(p)
        String(bytes, StandardCharsets.UTF_8)
    } catch (_: Throwable) {
        null
    }
}
