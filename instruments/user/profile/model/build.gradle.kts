import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("instruments.user.profile.model")
}

kotlin {
    commonDependencies {
        // No external dependencies needed for domain model
    }
}
