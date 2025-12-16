package ru.izhxx.aichallenge.core.safecall

import kotlinx.coroutines.test.runTest
import ru.izhxx.aichallenge.core.errors.AppError
import ru.izhxx.aichallenge.core.errors.ErrorRetry
import ru.izhxx.aichallenge.core.errors.ErrorSeverity
import ru.izhxx.aichallenge.core.errors.MetadataKey
import ru.izhxx.aichallenge.core.result.AppResult
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
                AppError.DomainError(
                    rawMessage = "mapped",
                    severity = ErrorSeverity.Error,
                    retry = ErrorRetry.Unknown
                )
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
            error.metadata[MetadataKey("origin")],
            "Default mapper must set ORIGIN metadata"
        )
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
        val mapped = AppError.DomainError(
            rawMessage = "mapped",
            severity = ErrorSeverity.Error,
            retry = ErrorRetry.Unknown
        )

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
