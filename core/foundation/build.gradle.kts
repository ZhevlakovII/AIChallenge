
import ru.izhxx.aichallenge.logic.commonTestDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("core.foundation")
}

kotlin {
    commonTestDependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutinesTest)
    }
}
