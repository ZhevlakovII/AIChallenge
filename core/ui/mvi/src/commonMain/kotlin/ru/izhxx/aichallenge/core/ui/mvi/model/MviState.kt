package ru.izhxx.aichallenge.core.ui.mvi.model

/**
 * Интерфейс‑маркер для состояния (State) в MVI для UI‑слоя.
 *
 * Назначение:
 * - State — единственный источник правды для экрана: все визуальные элементы должны
 *   опираться на поля этого состояния.
 * - State является результатом применения редьюсера (Reducer) к предыдущему состоянию и Result.
 *
 * Требования:
 * - Делайте State неизменяемым (immutability, `data class`).
 * - Держите только то, что необходимо для рендера (данные, флаги загрузки, ошибки для отображения).
 * - Не храните в State одноразовые события (навигация/тост/Snackbar) — они являются [MviEffect].
 * - Старайтесь моделировать отсутствие/наличие данных явно (например, sealed/nullable/Option-подход).
 *
 * Пример:
 * ```
 * data class SearchState(
 *   val query: String = "",
 *   val isLoading: Boolean = false,
 *   val items: List<Item> = emptyList(),
 *   val error: String? = null
 * ) : MviState
 * ```
 */
interface MviState
