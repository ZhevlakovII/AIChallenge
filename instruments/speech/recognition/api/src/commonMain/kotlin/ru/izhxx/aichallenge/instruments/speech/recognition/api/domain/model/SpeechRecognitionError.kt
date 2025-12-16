package ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.model

/**
 * Типы ошибок распознавания речи
 */
sealed class SpeechRecognitionError {
    /**
     * Отсутствует разрешение RECORD_AUDIO
     */
    data object NoPermission : SpeechRecognitionError()

    /**
     * Ошибка сети
     */
    data object NetworkError : SpeechRecognitionError()

    /**
     * Речь не обнаружена
     */
    data object NoSpeechDetected : SpeechRecognitionError()

    /**
     * Ошибка сервера распознавания
     */
    data object ServerError : SpeechRecognitionError()

    /**
     * Распознаватель речи недоступен на устройстве
     */
    data object RecognizerNotAvailable : SpeechRecognitionError()

    /**
     * Неизвестная ошибка
     * @param message Сообщение об ошибке
     */
    data class UnknownError(val message: String) : SpeechRecognitionError()
}
