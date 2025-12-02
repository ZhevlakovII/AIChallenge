import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}

android {
    config("ru.izhxx.aichallenge.features.pranalyzer.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.features.pranalyzer.api)
        implementation(projects.core.ui.mvi)
        implementation(projects.core.foundation)
        implementation(projects.shared.sharedold)

        // Koin DI
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)

        // Coroutines
        implementation(libs.kotlinx.coroutinesCore)

        // Serialization
        implementation(libs.kotlinx.serialization.json)

        // DateTime
        implementation(libs.kotlinx.datetime)

        // Compose
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)

        // AndroidX
        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.androidx.lifecycle.runtimeCompose)
    }
}
