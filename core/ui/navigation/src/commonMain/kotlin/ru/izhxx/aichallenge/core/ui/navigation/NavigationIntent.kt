package ru.izhxx.aichallenge.core.ui.navigation

/**
 * Интерфейс-маркер для навигационных намерений в системе навигации приложения.
 *
 * Назначение:
 * - Представляет команду навигации, которая должна быть обработана системой навигации.
 * - Используется совместно с [NavigationHandler] и [NavigationBus] для реализации навигации.
 * - Позволяет типобезопасно описать все навигационные действия в приложении.
 *
 * Требования по моделированию:
 * - Объявляйте намерения как sealed interface/sealed class на уровне модуля/фичи.
 * - Делайте намерения неизменяемыми (immutability).
 * - Включайте в намерения только данные, необходимые для навигации (id, параметры).
 * - Не смешивайте навигационные намерения с бизнес-логикой.
 *
 * Правила:
 * - Каждый модуль/фича определяет свои собственные навигационные намерения.
 * - Намерения отправляются через [NavigationBus.send].
 * - Обрабатываются через [NavigationHandler.handle].
 *
 * Пример:
 * ```kotlin
 * // В модуле features/profile:
 * sealed interface ProfileNavigationIntent : NavigationIntent {
 *     data class OpenProfile(val userId: String) : ProfileNavigationIntent
 *     object OpenSettings : ProfileNavigationIntent
 *     data class OpenEditProfile(val userId: String) : ProfileNavigationIntent
 * }
 *
 * // Использование:
 * navigationBus.send(ProfileNavigationIntent.OpenProfile("user123"))
 * ```
 *
 * @see NavigationHandler
 * @see NavigationBus
 */
interface NavigationIntent
