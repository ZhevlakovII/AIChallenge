package ru.izhxx.aichallenge.logic.configurator

import com.android.build.gradle.LibraryExtension
import ru.izhxx.aichallenge.logic.config.AndroidConfig
import ru.izhxx.aichallenge.logic.config.SharedConfig

/**
 * Настраивает базовые параметры для Android-библиотеки.
 * @param namespaceSuffix суффикс для namespace (по умолчанию имя проекта)
 */
fun LibraryExtension.config(namespaceSuffix: String) {
    namespace = "${SharedConfig.NAMESPACE}.$namespaceSuffix"
    compileSdk = AndroidConfig.COMPILE_SDK

    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = AndroidConfig.JAVA_VERSION
        targetCompatibility = AndroidConfig.JAVA_VERSION
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}