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
import ru.izhxx.aichallenge.data.database.dao.ReminderTaskDao
import ru.izhxx.aichallenge.data.database.dao.ReminderResultDao
import ru.izhxx.aichallenge.data.database.dao.McpServerDao
import ru.izhxx.aichallenge.data.database.entity.ReminderTaskEntity
import ru.izhxx.aichallenge.data.database.entity.ReminderResultEntity
import ru.izhxx.aichallenge.data.database.entity.McpServerEntity

/**
 * Основной класс базы данных Room для приложения
 */
@Database(
    entities = [
        MessageEntity::class,
        ChatHistoryEntity::class,
        DialogEntity::class,
        SummaryEntity::class,
        ReminderTaskEntity::class,
        ReminderResultEntity::class,
        McpServerEntity::class
    ],
    version = 9,
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

    /**
     * Возвращает DAO для работы с задачами-напоминаниями
     */
    abstract fun reminderTaskDao(): ReminderTaskDao

    /**
     * Возвращает DAO для работы с результатами напоминаний
     */
    abstract fun reminderResultDao(): ReminderResultDao

    /**
     * Возвращает DAO для работы со списком MCP-серверов
     */
    abstract fun mcpServerDao(): McpServerDao
}
