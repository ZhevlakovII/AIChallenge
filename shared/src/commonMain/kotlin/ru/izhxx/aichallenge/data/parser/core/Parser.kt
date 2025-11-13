package ru.izhxx.aichallenge.data.parser.core

/**
 * Базовый интерфейс для всех парсеров
 *
 * @param T тип входных данных
 * @param R тип результата
 */
interface Parser<in T, out R> {
    /**
     * Парсит входные данные и возвращает результат
     *
     * @param input входные данные
     * @return обернутый в Result результат парсинга
     */
    fun parse(input: T): Result<R>
}
