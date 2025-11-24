package ru.izhxx.aichallenge.core.ui.mvi.model

/**
 * Интерфейс‑маркер для состояния (State) в MVI для UI‑слоя.
 *
 * Назначение:
 * - State — единственный источник правды для экрана: все визуальные элементы должны
 *   опираться на поля этого состояния.
 * - State является результатом применения редьюсера (Reducer) к предыдущему состоянию и Result.
 *
 * Требования и рекомендации:
 * - Делайте State неизменяемым (immutability). Как правило, это `data class`.
 * - Держите только то, что необходимо для рендера (данные, флаги загрузки, ошибки для отображения).
 * - Не храните в State одноразовые события (навигация/тост/Snackbar) — они являются [MviEffect].
 * - Старайтесь моделировать отсутствие/наличие данных явно (например, sealed/nullable/Option-подход).
 *
 * Поток данных (упрощённо):
 * Intent → Executor → Result → Reducer(State × Result → State) → Обновлённый State → UI.
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
public interface MviState
