package ru.izhxx.aichallenge.data.preferences

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android-реализация [LLMSettingsStoreFactory]
 */
actual class LLMSettingsStoreFactory : KoinComponent {
    /**
     * Создает и возвращает экземпляр AndroidLLMSettingsStore
     */
    actual fun create(): LLMSettingsStore {
        val context: Context by inject()
        return AndroidLLMSettingsStore(context)
    }
}
