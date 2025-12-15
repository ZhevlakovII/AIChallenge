import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("instruments.user.profile.repository.api")
}

kotlin {
    commonDependencies {
        api(projects.instruments.user.profile.model)
    }
}
