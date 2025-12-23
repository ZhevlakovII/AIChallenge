package ru.izhxx.aichallenge.tools.embedder.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

@OptIn(ExperimentalForeignApi::class)
internal actual fun ByteArray.sha256(): String {
    val digest = UByteArray(size)
    usePinned { pinned ->
        digest.usePinned { digestPinned ->
            CC_SHA256(pinned.addressOf(0), size.toUInt(), digestPinned.addressOf(0))
        }
    }
    return digest.joinToString("") { string ->
        NSString.stringWithFormat("%02x", string)
    }
}