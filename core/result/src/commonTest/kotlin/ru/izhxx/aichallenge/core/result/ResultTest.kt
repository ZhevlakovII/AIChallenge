package ru.izhxx.aichallenge.core.result

import ru.izhxx.aichallenge.core.errors.AppError
import ru.izhxx.aichallenge.core.errors.ErrorRetry
import ru.izhxx.aichallenge.core.errors.ErrorSeverity
import ru.izhxx.aichallenge.core.errors.MetadataKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultTest {

    @Test
    fun `onSuccess invokes and chains`() {
        var testData: Int? = null
        val successResult: AppResult<Int> = AppResult.success(42)

        val chain = successResult
            .onSuccess { testData = it }
            .onFailure { /* no-op */ }

        assertEquals(42, testData)
        // Должно возвращать тот же объект (по контракту chain)
        assertTrue(chain is AppResult.Success)
        assertEquals(42, chain.value)
    }

    @Test
    fun `onFailure invokes and chains`() {
        var testData: AppError? = null
        val domainError = AppError.DomainError(
            rawMessage = "Booom!",
            severity = ErrorSeverity.Warning,
            retry = ErrorRetry.Unknown,
            metadata = mapOf(
                MetadataKey("domain.test") to ResultTest::class.simpleName.orEmpty()
            )
        )
        val failureResult: AppResult<Int> = AppResult.failure(domainError)

        val chain = failureResult
            .onFailure { testData = it }
            .onSuccess { /* no-op */ }

        assertEquals(domainError, testData)
        assertTrue(chain is AppResult.Failure)
        assertEquals(domainError, chain.error)
    }

    @Test
    fun `getSuccessOrNull returns correct value`() {
        val successResult: AppResult<Int> = AppResult.success(7)
        val failureResult: AppResult<Int> = AppResult.failure(
            AppError.UnknownError(
                rawMessage = "x",
                severity = ErrorSeverity.Warning,
                retry = ErrorRetry.Unknown,
            )
        )

        assertEquals(7, successResult.getSuccessOrNull())
        assertNull(failureResult.getSuccessOrNull())
    }
}
