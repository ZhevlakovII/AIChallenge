package ru.izhxx.aichallenge.instruments.speech.recognition.impl.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.izhxx.aichallenge.instruments.speech.recognition.api.domain.usecase.RecognizeSpeechUseCase
import ru.izhxx.aichallenge.instruments.speech.recognition.impl.domain.usecase.RecognizeSpeechUseCaseImpl

actual val speechRecognitionModule: Module = module {
    factory<RecognizeSpeechUseCase> { RecognizeSpeechUseCaseImpl(get()) }
}