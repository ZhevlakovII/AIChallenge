package ru.izhxx.aichallenge.core.mvi.runtime

import ru.izhxx.aichallenge.core.mvi.model.MviResult
import ru.izhxx.aichallenge.core.mvi.model.MviState

/**
 * Редьюсер (Reducer) — чистая функция свёртки [MviResult] в новое [MviState].
 *
 * Назначение:
 * - Инкапсулирует детерминированную логику преобразования: (текущее состояние, результат)
 *   → новое состояние.
 * - Обеспечивает предсказуемость: одинаковые входы дают одинаковый выход без сайд‑эффектов.
 *
 * Правила:
 * - Не должен выполнять побочные эффекты, вызывать IO, менять внешние объекты.
 * - Должен возвращать новый экземпляр состояния (immutability); избегайте мутаций.
 *
 * Типичный конвейер:
 * Intent → (Executor) → Result → Reducer(State × Result → State) → UI рендерит State.
 *
 * Рекомендации:
 * - Храните только логику преобразования данных в State. Любые асинхронные операции/побочки —
 *   вне редьюсера (например, в исполнителях/внутри ViewModel).
 *
 * Пример:
 * ```
 * val reducer = MviReducer<SearchState, SearchResult> { state, result ->
 *   when (result) {
 *     is SearchResult.Loading -> state.copy(isLoading = true, error = null)
 *     is SearchResult.Data -> state.copy(isLoading = false, items = result.items, error = null)
 *     is SearchResult.Error -> state.copy(isLoading = false, error = result.message)
 *   }
 * }
 * ```
 */
public fun interface MviReducer<S : MviState, R : MviResult> {
    /**
     * Выполнить свёртку результата [result] с текущим состоянием [state] в новое состояние.
     */
    public fun reduce(state: S, result: R): S
}
