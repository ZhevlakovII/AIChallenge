import gradle.kotlin.dsl.accessors._1357380998321d090f37c7e11a288dc8.kotlin
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.extensions.libs

/*
 * Convention plugin: kmp.library
 * Базовая KMP Library конфигурация.
 * Включает в себя таргеты: Android, iOS, JVM
 */

plugins {
    id("androidx.room")
    id("com.google.devtools.ksp")
}

/*
 * Android config применяется непосредственно в модуле, так как необходимо передавать module name
 */

kotlin {
    commonDependencies {
        implementation(libs.room.runtime)
        implementation(libs.sqlite.bundled)
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
