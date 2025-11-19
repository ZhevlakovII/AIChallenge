package ru.izhxx.aichallenge.di

import org.koin.core.module.Module

/**
 * Платформенный DI-модуль.
 *
 * expect-объявление позволяет подключать платформенные реализации
 * (android/jvm/desktop и др.) в общем коде.
 */
expect val platformModule: Module
