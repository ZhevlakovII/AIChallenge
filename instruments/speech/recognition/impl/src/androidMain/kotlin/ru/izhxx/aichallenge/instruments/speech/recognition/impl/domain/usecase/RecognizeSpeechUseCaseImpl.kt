package ru.izhxx.aichallenge.instruments.speech.recognition.impl.domain.usecase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import ru.izhxx.aichallenge.core.logger.Tag
import ru.izhxx.aichallenge.core.logger.debug
import ru.izhxx.aichallenge.core.logger.error
import ru.izhxx.aichallenge.core.logger.warn
import ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.model.SpeechRecognitionError
import ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.model.SpeechRecognitionResult
import ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.usecase.RecognizeSpeechUseCase

/**
 * Android-реализация распознавания речи с использованием SpeechRecognizer API
 */
class RecognizeSpeechUseCaseImpl(
    private val context: Context
) : RecognizeSpeechUseCase {

    private val loggerTag = Tag.ofTruncated(RecognizeSpeechUseCaseImpl::class.simpleName.orEmpty())

    private var speechRecognizer: SpeechRecognizer? = null

    override fun startRecognition(languageCode: String): Flow<SpeechRecognitionResult> = callbackFlow {
        // Проверяем доступность распознавателя
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error(loggerTag,null) {
                "Speech recognition is not available on this device"
            }
            trySend(SpeechRecognitionResult.Error(SpeechRecognitionError.RecognizerNotAvailable))
            close()
            return@callbackFlow
        }

        // Проверяем разрешение
        if (!hasPermission()) {
            error(loggerTag,null) {
                "RECORD_AUDIO permission not granted"
            }
            trySend(SpeechRecognitionResult.Error(SpeechRecognitionError.NoPermission))
            close()
            return@callbackFlow
        }

        // SpeechRecognizer должен быть создан на главном потоке
        withContext(Dispatchers.Main) {
            try {
                // Создаем SpeechRecognizer
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            debug(loggerTag) {
                                "onReadyForSpeech"
                            }
                            trySend(SpeechRecognitionResult.ReadyForSpeech)
                        }

                        override fun onBeginningOfSpeech() {
                            debug(loggerTag) {
                                "onBeginningOfSpeech"
                            }
                            trySend(SpeechRecognitionResult.Listening)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Уровень громкости - можно использовать для визуализации
                            // Пока не отправляем, чтобы не перегружать Flow
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {
                            // Буфер аудио - не используем
                        }

                        override fun onEndOfSpeech() {
                            debug(loggerTag) {
                                "onEndOfSpeech"
                            }
                            // Речь закончена, ожидаем результаты
                        }

                        override fun onError(error: Int) {
                            error(loggerTag) {
                                "onError: $error"
                            }
                            val speechError = when (error) {
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognitionError.NetworkError

                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionError.NoSpeechDetected

                                SpeechRecognizer.ERROR_SERVER -> SpeechRecognitionError.ServerError

                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionError.NoPermission

                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                                SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionError.RecognizerNotAvailable

                                else -> SpeechRecognitionError.UnknownError("Error code: $error")
                            }

                            trySend(SpeechRecognitionResult.Error(speechError))
                            close()
                        }

                        override fun onResults(results: Bundle?) {
                            debug(loggerTag) {
                                "onResults"
                            }
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                            if (matches.isNullOrEmpty()) {
                                warn(loggerTag) {
                                    "No recognition results"
                                }
                                trySend(SpeechRecognitionResult.Error(SpeechRecognitionError.NoSpeechDetected))
                            } else {
                                val text = matches[0]
                                val confidence = confidenceScores?.getOrNull(0) ?: 1.0f
                                debug(loggerTag) {
                                    "Recognition success: $text (confidence: $confidence)"
                                }
                                trySend(SpeechRecognitionResult.Success(text, confidence))
                            }

                            close()
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            debug(loggerTag) {
                                "onPartialResults"
                            }
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                            if (!matches.isNullOrEmpty()) {
                                val partialText = matches[0]
                                debug(loggerTag) {
                                    "Partial result: $partialText"
                                }
                                trySend(SpeechRecognitionResult.PartialResult(partialText))
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {
                            // Дополнительные события - не используем
                        }
                    })
                }

                // Создаем Intent для распознавания
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Включаем частичные результаты
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                // Запускаем распознавание
                speechRecognizer?.startListening(intent)
                debug(loggerTag) {
                    "Speech recognition started"
                }

            } catch (e: Exception) {
                error(loggerTag, e) {
                    "Failed to start speech recognition"
                }
                trySend(SpeechRecognitionResult.Error(
                    SpeechRecognitionError.UnknownError(e.message ?: "Unknown error")
                ))
                close()
            }
        }

        awaitClose {
            debug(loggerTag) {
                "Closing speech recognition flow"
            }
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                error(loggerTag, e) {
                    "Error closing speech recognizer"
                }
            }
        }
    }

    override suspend fun stopRecognition() {
        debug(loggerTag) {
            "stopRecognition called"
        }
        withContext(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                error(loggerTag, e) {
                    "Error stopping speech recognizer"
                }
            }
        }
    }

    override suspend fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override suspend fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
