import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("ru.izhxx.aichallenge.features.pranalyzer.api")
}

kotlin {
    // Пустой API модуль пока
}
