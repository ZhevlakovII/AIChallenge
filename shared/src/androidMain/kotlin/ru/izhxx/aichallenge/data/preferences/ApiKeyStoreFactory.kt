package ru.izhxx.aichallenge.data.preferences

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android-реализация [ApiKeyStoreFactory]
 */
actual class ApiKeyStoreFactory : KoinComponent {
    /**
     * Создает и возвращает экземпляр AndroidApiKeyStore
     */
    actual fun create(): ApiKeyStore {
        val context: Context by inject()
        return AndroidApiKeyStore(context)
    }
}
