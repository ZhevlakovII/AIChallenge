package ru.izhxx.aichallenge

import android.app.Application
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import ru.izhxx.aichallenge.di.appModule
import ru.izhxx.aichallenge.di.platformModule
import ru.izhxx.aichallenge.domain.service.ReminderEngine

/**
 * Главный класс приложения для Android
 */
class MainApplication : Application() {

    private val reminderEngine: ReminderEngine by inject()

    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Koin
        startKoin {
            // Логирование для отладки
            androidLogger(Level.ERROR)
            
            // Передаем контекст приложения в Koin
            androidContext(this@MainApplication)
            
            // Загружаем модули
            modules(appModule, platformModule)
        }

        reminderEngine.start()
    }
}
