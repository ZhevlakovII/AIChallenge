package ru.izhxx.aichallenge.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.izhxx.aichallenge.domain.service.AndroidReminderNotifier
import ru.izhxx.aichallenge.domain.service.ReminderNotifier

/**
 * Android-специфичный DI-модуль.
 *
 * Переопределяет биндинг ReminderNotifier на Android-реализацию,
 * чтобы показывать системные уведомления.
 */
actual val platformModule: Module = module {
    single<ReminderNotifier> {
        AndroidReminderNotifier(get())
    }
}
