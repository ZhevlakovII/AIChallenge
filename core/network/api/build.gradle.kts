import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("core.network.api")
}

kotlin {
    commonDependencies {
        implementation(libs.ktor.clientCore)
    }
}
