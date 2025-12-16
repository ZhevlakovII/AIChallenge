package ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.model.SpeechRecognitionResult

/**
 * Use case для распознавания речи
 */
interface RecognizeSpeechUseCase {
    /**
     * Начинает распознавание речи и возвращает Flow с результатами
     * Flow позволяет получать состояния в реальном времени (частичные результаты, состояния)
     *
     * @param languageCode Код языка для распознавания (например, "ru-RU", "en-US")
     * @return Flow с результатами распознавания
     */
    fun startRecognition(languageCode: String = "ru-RU"): Flow<SpeechRecognitionResult>

    /**
     * Останавливает текущее распознавание речи
     */
    suspend fun stopRecognition()

    /**
     * Проверяет, доступно ли распознавание речи на устройстве
     *
     * @return true если распознавание доступно, false иначе
     */
    suspend fun isRecognitionAvailable(): Boolean

    /**
     * Проверяет, есть ли разрешение RECORD_AUDIO
     *
     * @return true если разрешение предоставлено, false иначе
     */
    suspend fun hasPermission(): Boolean
}
