import ru.izhxx.aichallenge.logic.androidDependencies
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.iosDependencies
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("kmp.library")
}

android {
    config("core.network.core.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.core.network.core.api)
        implementation(libs.ktor.clientCore)
        implementation(libs.ktor.clientContentNegotiation)
        implementation(libs.ktor.serializationKotlinxJson)
        implementation(libs.ktor.clientLogging)
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.koin.core)
    }

    androidDependencies {
        implementation(libs.ktor.clientOkhttp)
    }

    jvmDependencies {
        implementation(libs.ktor.clientCio)
    }

    iosDependencies {
        implementation(libs.ktor.clientDarwin)
    }
}
