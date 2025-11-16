package ru.izhxx.aichallenge.data.database

/**
 * Фабрика для создания экземпляра базы данных
 */
expect object DatabaseFactory {

    /**
     * Возвращает экземпляр базы данных
     */
    fun getDatabase(): AppDatabase
}

const val DATABASE_NAME = "aichallenge_database"
