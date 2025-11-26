import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("core.safecall")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.coroutinesCore)
    }
}
