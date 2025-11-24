import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("frontend.library")
}

android {
    // Namespace будет ru.izhxx.aichallenge.shared.core.logger
    config("core.logger")
}

kotlin {
    commonDependencies {
        implementation(libs.atomicfu)
    }
    jvmDependencies {
        implementation(libs.slf4j)
    }
}
