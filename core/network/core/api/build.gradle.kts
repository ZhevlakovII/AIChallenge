import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("core.network.core.api")
}

kotlin {
    commonDependencies {
        api(projects.core.foundation)
        implementation(libs.kotlinx.coroutinesCore)
    }
}
