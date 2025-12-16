import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("core.dispatchers.api")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.coroutines.core)
    }
}
