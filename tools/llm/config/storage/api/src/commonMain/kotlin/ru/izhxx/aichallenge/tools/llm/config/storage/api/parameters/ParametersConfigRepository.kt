package ru.izhxx.aichallenge.tools.llm.config.storage.api.parameters

import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig

/**
 * Интерфейс репозитория для управления параметрами генерации LLM.
 *
 * Назначение:
 * - Предоставляет абстракцию для хранения и получения параметров генерации LLM.
 * - Позволяет обновлять отдельные параметры
 * (temperature, max_tokens и т.д.) или всю конфигурацию.
 * - Используется для персистентного хранения пользовательских настроек генерации.
 *
 * Применение:
 * - Используйте для сохранения и загрузки настроек генерации LLM.
 * - Реализация обычно использует DataStore или базу данных для персистентности.
 * - Все операции асинхронные через suspend функции.
 *
 * Параметры генерации:
 * - temperature - управляет креативностью ответов (0.0 - детерминированный, 1.0 - творческий).
 * - maxTokens - ограничивает длину генерируемого ответа.
 * - topK, topP, minP, topA - параметры семплирования токенов.
 * - seed - для воспроизводимости результатов.
 *
 * Правила:
 * - Параметры должны сохраняться между запусками приложения.
 * - При первом запуске должны использоваться значения по умолчанию.
 * - Все методы update должны сохранять изменения немедленно.
 *
 * Пример:
 * ```kotlin
 * class SettingsViewModel(
 *     private val repository: ParametersConfigRepository
 * ) {
 *     suspend fun loadParameters() {
 *         val config = repository.getConfig()
 *         _temperatureState.value = config.temperature
 *     }
 *
 *     suspend fun onTemperatureChanged(newValue: Double) {
 *         repository.updateTemperature(newValue)
 *     }
 * }
 * ```
 *
 * @see ParametersConfig
 */
interface ParametersConfigRepository {

    /**
     * Получает текущую конфигурацию параметров генерации.
     *
     * @return Текущая конфигурация параметров.
     */
    suspend fun getConfig(): ParametersConfig

    /**
     * Обновляет параметр temperature (температура генерации).
     *
     * @param temperature Новое значение temperature (обычно 0.0 - 1.0).
     */
    suspend fun updateTemperature(temperature: Double)

    /**
     * Обновляет максимальное количество токенов в ответе.
     *
     * @param maxTokens Новое значение максимального количества токенов.
     */
    suspend fun updateMaxTokens(maxTokens: Int)

    /**
     * Обновляет параметр Top-K для семплирования.
     *
     * @param topK Новое значение Top-K.
     */
    suspend fun updateTopK(topK: Int)

    /**
     * Обновляет параметр Top-P (nucleus sampling).
     *
     * @param topP Новое значение Top-P (обычно 0.0 - 1.0).
     */
    suspend fun updateTopP(topP: Double)

    /**
     * Обновляет параметр Min-P (минимальный порог вероятности).
     *
     * @param minP Новое значение Min-P.
     */
    suspend fun updateMinP(minP: Double)

    /**
     * Обновляет параметр Top-A для семплирования.
     *
     * @param topA Новое значение Top-A.
     */
    suspend fun updateTopA(topA: Double)

    /**
     * Обновляет seed для воспроизводимости результатов.
     *
     * @param seed Новое значение seed.
     */
    suspend fun updateSeed(seed: Long)

    /**
     * Обновляет всю конфигурацию параметров целиком.
     *
     * @param newConfig Новая конфигурация параметров.
     */
    suspend fun updateConfig(newConfig: ParametersConfig)
}