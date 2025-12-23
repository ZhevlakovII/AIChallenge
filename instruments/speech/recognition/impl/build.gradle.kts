import ru.izhxx.aichallenge.logic.androidDependencies
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("instruments.speech.recognition.impl")
}

kotlin {
    commonDependencies {
        // Project
        implementation(projects.instruments.speech.recognition.api)
        implementation(projects.core.foundation)
        implementation(projects.core.logger)

        // Libraries
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.core)
    }

    androidDependencies {
        // Android-specific dependencies for SpeechRecognizer (все встроены в Android SDK)
        implementation(libs.androidx.core.ktx)
    }
}
