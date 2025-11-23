package ru.izhxx.aichallenge.logic.config

import org.gradle.api.JavaVersion

object AndroidConfig {
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
    const val COMPILE_SDK = 36

    val JAVA_VERSION = JavaVersion.VERSION_21
}