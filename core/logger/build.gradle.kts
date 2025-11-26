import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.commonTestDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("core.logger")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.datetime)
    }
    commonTestDependencies {
        implementation(kotlin("test"))
    }
}
