package ru.izhxx.aichallenge

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import ru.izhxx.aichallenge.di.appModule

/**
 * Главный класс приложения для Android
 */
class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Koin
        startKoin {
            // Логирование для отладки
            androidLogger(Level.ERROR)
            
            // Передаем контекст приложения в Koin
            androidContext(this@MainApplication)
            
            // Загружаем модули
            modules(appModule)
        }
    }
}
