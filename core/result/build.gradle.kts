import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.commonTestDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("core.result")
}

kotlin {
    commonDependencies {
        implementation(projects.core.errors)
    }
    commonTestDependencies {
        implementation(libs.kotlin.test)
    }
}
