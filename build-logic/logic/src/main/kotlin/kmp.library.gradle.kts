import ru.izhxx.aichallenge.logic.configurator.config

/*
 * Convention plugin: kmp.library
 * Базовая KMP Library конфигурация.
 * Включает в себя таргеты: Android, iOS, JVM
 */

plugins {
    kotlin("multiplatform")
    id("com.android.library")
//    id("lint")
}

/*
 * Android config применяется непосредственно в модуле, так как необходимо передавать module name
 */

kotlin {
    config()
}
