package ru.izhxx.aichallenge.data.database

import androidx.room.Room
import java.io.File

actual object DatabaseFactory {

    actual fun getDatabase(): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(
            name = File(System.getProperty("java.io.tmpdir"), "my_room.db").absolutePath
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
}
