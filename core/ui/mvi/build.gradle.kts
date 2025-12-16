import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("core.ui.mvi")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.coroutines.core)
    }
}
