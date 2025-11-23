import ru.izhxx.aichallenge.logic.configurator.config

/*
 * Convention plugin: frontend.library
 * Базовая KMP Library конфигурация.
 */

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

/*
 * Android config применяется непосредственно в модуле, так как необходимо передавать module name
 */

kotlin {
    config()
}
