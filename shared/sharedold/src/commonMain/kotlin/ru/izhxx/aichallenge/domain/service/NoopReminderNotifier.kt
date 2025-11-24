package ru.izhxx.aichallenge.domain.service

/**
 * No-op реализация ReminderNotifier для общих сборок.
 * Платформенные модули могут переопределить биндинг Koin своей реализацией.
 */
class NoopReminderNotifier : ReminderNotifier {
    override fun notifyResult(taskId: Long, resultId: Long, title: String, preview: String) {
        // Ничего не делаем по умолчанию.
    }
}
