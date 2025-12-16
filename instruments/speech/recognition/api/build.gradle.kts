import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("instruments.speech.recognition.api")
}

kotlin {
    commonDependencies {
        // Libraries
        implementation(libs.kotlinx.coroutines.core)
    }
}
