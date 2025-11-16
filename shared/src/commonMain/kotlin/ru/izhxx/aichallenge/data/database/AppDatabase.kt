package ru.izhxx.aichallenge.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.izhxx.aichallenge.data.database.dao.ChatHistoryDao
import ru.izhxx.aichallenge.data.database.dao.DialogDao
import ru.izhxx.aichallenge.data.database.dao.MessageDao
import ru.izhxx.aichallenge.data.database.dao.SummaryDao
import ru.izhxx.aichallenge.data.database.entity.ChatHistoryEntity
import ru.izhxx.aichallenge.data.database.entity.DialogEntity
import ru.izhxx.aichallenge.data.database.entity.MessageEntity
import ru.izhxx.aichallenge.data.database.entity.SummaryEntity

/**
 * Основной класс базы данных Room для приложения
 */
@Database(
    entities = [
        MessageEntity::class,
        ChatHistoryEntity::class,
        DialogEntity::class,
        SummaryEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Возвращает DAO для работы с сообщениями
     */
    abstract fun messageDao(): MessageDao
    
    /**
     * Возвращает DAO для работы с диалогами
     */
    abstract fun dialogDao(): DialogDao
    
    /**
     * Возвращает DAO для работы с саммари
     */
    abstract fun summaryDao(): SummaryDao

    /**
     * Возвращает DAO для работы с историей чата
     */
    abstract fun chatHistoryDao(): ChatHistoryDao
}
