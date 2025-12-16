import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.commonTestDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("core.safecall")
}

kotlin {
    commonDependencies {
        implementation(projects.core.errors)
        implementation(projects.core.result)
    }
    commonTestDependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}
