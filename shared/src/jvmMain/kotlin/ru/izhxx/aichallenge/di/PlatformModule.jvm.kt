package ru.izhxx.aichallenge.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * JVM/desktop-реализация платформенного DI-модуля.
 *
 * На JVM уведомления не реализуются, поэтому модуль пустой.
 * Общие биндинги заданы в sharedModule.
 */
actual val platformModule: Module = module { }
