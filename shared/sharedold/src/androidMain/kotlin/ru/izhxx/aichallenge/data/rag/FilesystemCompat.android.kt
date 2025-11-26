package ru.izhxx.aichallenge.data.rag

import java.io.File

actual fun readFileTextCompat(path: String): String? {
    return try {
        val f = File(path)
        if (!f.exists() || !f.isFile) return null
        f.readText(Charsets.UTF_8)
    } catch (_: Throwable) {
        null
    }
}
