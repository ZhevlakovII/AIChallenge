package ru.izhxx.aichallenge.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Интерфейс для создания и конфигурации Ktor HttpClient.
 *
 * Назначение:
 * - Абстракция над созданием HttpClient для упрощения тестирования и конфигурации.
 * - Централизованное управление настройками HTTP клиента (таймауты, логирование, плагины).
 * - Позволяет создавать различные конфигурации клиента для разных сервисов или окружений.
 *
 * Особенности:
 * - Поддержка платформо-специфичных движков (Android, iOS, JVM).
 * - Возможность добавления кастомных плагинов и интерцепторов через DSL блок.
 * - Автоматическая настройка логирования, тайм-аутов и других общих параметров.
 *
 * Правила использования:
 * - Внедряйте [HttpClientCreator] через конструктор в классы, использующие HTTP клиент.
 * - Используйте [buildHttpClient] для создания клиента с кастомной конфигурацией.
 * - Настраивайте плагины (ContentNegotiation, Logging, Auth) через DSL блок.
 * - Не создавайте HttpClient напрямую через HttpClient() — всегда используйте этот интерфейс.
 *
 * Пример:
 * ```kotlin
 * class ApiService(private val httpClientCreator: HttpClientCreator) {
 *     private val client = httpClientCreator.buildHttpClient {
 *         install(ContentNegotiation) {
 *             json()
 *         }
 *         install(Logging) {
 *             level = LogLevel.BODY
 *         }
 *         defaultRequest {
 *             url("https://api.example.com/")
 *         }
 *     }
 *
 *     suspend fun fetchData(): String = client.get("endpoint").bodyAsText()
 * }
 * ```
 *
 * @see HttpClient
 * @see HttpClientConfig
 */
interface HttpClientCreator {

    /**
     * Создает новый HttpClient с заданной конфигурацией.
     *
     * @param block DSL блок для конфигурации HttpClient (плагины, настройки, интерцепторы).
     * @return Настроенный экземпляр HttpClient.
     */
    fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
}
