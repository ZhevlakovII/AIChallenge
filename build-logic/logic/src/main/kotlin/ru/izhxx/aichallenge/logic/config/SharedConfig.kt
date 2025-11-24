package ru.izhxx.aichallenge.logic.config

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object SharedConfig {
    // Единый идентификатор пакета (используется как namespace/applicationId/packageName)
    const val NAMESPACE = "ru.izhxx.aichallenge"
    // Версия приложения (общая семантическая версия)
    const val VERSION_NAME = "1.2"

    // Целевой уровень JVM для Kotlin компилятора
    val JVM_TARGET: JvmTarget = JvmTarget.JVM_21

}
