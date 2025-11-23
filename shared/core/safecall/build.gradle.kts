import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("frontend.library")
}

android {
    // Namespace будет ru.izhxx.aichallenge.shared.core.safecall
    config("shared.core.safecall")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.coroutinesCore)
    }
}
