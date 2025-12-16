package ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.model

/**
 * Результат распознавания речи
 */
sealed class SpeechRecognitionResult {
    /**
     * Успешное распознавание речи
     * @param text Распознанный текст
     * @param confidence Уровень уверенности в распознавании (0.0-1.0)
     */
    data class Success(
        val text: String,
        val confidence: Float = 1.0f
    ) : SpeechRecognitionResult()

    /**
     * Частичный результат распознавания (в реальном времени)
     * @param text Частично распознанный текст
     */
    data class PartialResult(
        val text: String
    ) : SpeechRecognitionResult()

    /**
     * Ошибка распознавания
     * @param error Тип ошибки
     */
    data class Error(
        val error: SpeechRecognitionError
    ) : SpeechRecognitionResult()

    /**
     * Распознаватель начал слушать речь
     */
    data object Listening : SpeechRecognitionResult()

    /**
     * Распознаватель готов к приему речи
     */
    data object ReadyForSpeech : SpeechRecognitionResult()
}
