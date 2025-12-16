
import ru.izhxx.aichallenge.logic.androidDependencies
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("shared.library")
}

android {
    config("core.dispatchers.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.core.dispatchers.api)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.core)
    }
    androidDependencies {
        implementation(libs.kotlinx.coroutines.android)
    }
    jvmDependencies {
        implementation(libs.kotlinx.coroutines.swing)
    }
}
