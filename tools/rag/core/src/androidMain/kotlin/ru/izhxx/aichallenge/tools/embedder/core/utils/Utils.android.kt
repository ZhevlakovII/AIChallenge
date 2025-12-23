package ru.izhxx.aichallenge.tools.embedder.core.utils

import java.security.MessageDigest

internal actual fun ByteArray.sha256(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest()
        .joinToString("") { string ->
            "%02x".format(string)
        }
}
