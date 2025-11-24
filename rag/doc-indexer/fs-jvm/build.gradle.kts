plugins {
    id("backend.library")
    application // не делает модуль приложением, но даёт доступ к Java toolchain при необходимости
}

dependencies {
    implementation(project(":rag:doc-indexer:core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutinesCore)
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
    }
}
