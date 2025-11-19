package ru.izhxx.aichallenge.features.reminder.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.reminder.presentation.ReminderViewModel

/**
 * DI-модуль фичи Reminder.
 *
 * Регистрирует ViewModel фичи, использующую доменный репозиторий и движок.
 */
val reminderModule = module {
    viewModel {
        ReminderViewModel(
            repository = get(),
            executeReminderTask = get(),
            notifier = get(),
            engine = get()
        )
    }
}
