package ru.izhxx.aichallenge.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Фабрика для создания экземпляра базы данных
 */
actual object DatabaseFactory : KoinComponent {

    actual fun getDatabase(): AppDatabase {
        val context: Context by inject()

        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }
}
