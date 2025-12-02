package ru.izhxx.aichallenge.core.foundation.safecall

import kotlinx.coroutines.test.runTest
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.foundation.error.MetadataKeys
import ru.izhxx.aichallenge.core.foundation.result.AppResult
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SafeCallTest {

    @Test
    fun `safeCall success returns Success and does not invoke mapper`() {
        var mapperCalls = 0

        val result = safeCall(
            throwableMapper = {
                mapperCalls += 1
                AppError.DomainError(code = "mapped.error", rawMessage = "mapped")
            }
        ) {
            123
        }

        assertTrue(result is AppResult.Success)
        assertEquals(123, result.value)
        assertEquals(0, mapperCalls, "Throwable mapper must not be called on success")
    }

    @Test
    fun `safeCall exception maps to UnknownError by default with origin`() {
        val ex = IllegalStateException("oops")

        val result = safeCall { throw ex }

        assertTrue(result is AppResult.Failure)
        val error = result.error
        assertTrue(error is AppError.UnknownError, "Default mapper should produce UnknownError")
        assertEquals("oops", error.rawMessage)
        assertEquals(ex, error.cause)
        assertEquals(
            "core.foundation.safecall",
            error.metadata[MetadataKeys.ORIGIN],
            "Default mapper must set ORIGIN metadata"
        )
        assertEquals("unknown.unexpected", error.code)
    }

    @Test
    fun `safeCall rethrows CancellationException`() {
        assertFailsWith<CancellationException> {
            safeCall { throw CancellationException("cancel") }
        }
    }

    // ---- suspendedSafeCall ----

    @Test
    fun `suspendedSafeCall success returns Success`() = runTest {
        val result = suspendedSafeCall { "ok" }

        assertTrue(result is AppResult.Success)
        assertEquals("ok", result.value)
    }

    @Test
    fun `suspendedSafeCall exception uses custom mapper`() = runTest {
        val ex = IllegalArgumentException("bad")
        val mapped = AppError.DomainError(code = "domain.bad", rawMessage = "mapped")

        val result = suspendedSafeCall(
            throwableMapper = { t ->
                assertEquals(ex, t)
                mapped
            }
        ) {
            throw ex
        }

        assertTrue(result is AppResult.Failure)
        assertEquals(mapped, result.error)
    }

    @Test
    fun `suspendedSafeCall rethrows CancellationException`() = runTest {

        assertFailsWith<CancellationException> {
            suspendedSafeCall { throw CancellationException("cancel-s") }
        }
    }
}
