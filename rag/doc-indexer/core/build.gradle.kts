
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.commonTestDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("shared.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Namespace будет ru.izhxx.aichallenge.rag.docindexer.core
    config("rag.docindexer.core")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
    }
    jvmDependencies {
        // jvm-специфичных зависимостей пока нет
    }
    commonTestDependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}
