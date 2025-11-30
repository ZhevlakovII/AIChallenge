import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    config("core.ui.navigation")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}