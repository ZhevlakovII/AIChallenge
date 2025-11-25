package ru.izhxx.aichallenge.core.ui.mvi.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.izhxx.aichallenge.core.ui.mvi.model.MviEffect
import ru.izhxx.aichallenge.core.ui.mvi.model.MviIntent
import ru.izhxx.aichallenge.core.ui.mvi.model.MviResult
import ru.izhxx.aichallenge.core.ui.mvi.model.MviState

/**
 * Контракт MVI‑ViewModel для UI‑слоя (KMP, Compose Multiplatform, Android/iOS/Desktop).
 *
 * Назначение:
 * - Единая точка оркестрации MVI для конкретного экрана/фичи.
 * - Хранит текущее [state] как единственный источник правды.
 * - Принимает входящие [MviIntent] через [accept], запускает исполнение (через исполнителя‑слой),
 *   преобразует результаты [MviResult] редьюсером в новое [MviState], эмитит одноразовые [MviEffect] в [effects].
 *
 * Минимальный конвейер (без Action‑слоя):
 * UI → Intent → (Executor) → Result/Effect → Reducer(State × Result → State) → UI.
 *
 * Свойства:
 * - [state] — долгоживущий поток состояний для рендера (обычно наблюдается в Compose через collectAsState()).
 * - [effects] — одноразовые события (навигация, сообщения, диалоги), не хранятся в [MviState] и
 *   не должны воспроизводиться повторно при пересоздании подписки.
 *
 * Правила и рекомендации:
 * - [MviState] должен быть неизменяемым (immutability). Обновление состояния происходит только
 *   через редьюсер по входящему [MviResult].
 * - [MviEffect] используйте для всего, что не должно быть частью долгоживущего состояния.
 * - [accept] должна быть потокобезопасной; реализация обычно опирается на корутины/Flow.
 *
 * Рекомендации по использованию в Compose:
 * - Подписывайтесь на [state]: `val uiState by viewModel.state.collectAsState()`.
 * - Подписывайтесь на [effects] в `LaunchedEffect(viewModel)` и обрабатывайте каждое событие один раз.
 *
 * Типизация:
 * - Для каждого экрана определяются собственные sealed‑иерархии типов [I], [R], [S], [E].
 *
 * Пример (схема):
 * ```
 * interface SearchViewModel : MviViewModel<SearchIntent, SearchResult, SearchState, SearchEffect>
 *
 * // В UI (Compose):
 * val state by vm.state.collectAsState()
 * LaunchedEffect(vm) {
 *   vm.effects.collect { effect ->
 *     when (effect) {
 *       is SearchEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
 *       is SearchEffect.NavigateToDetails -> navigator.openDetails(effect.id)
 *     }
 *   }
 * }
 * ```
 */
public interface MviViewModel<I : MviIntent, R : MviResult, S : MviState, E : MviEffect> {
    /**
     * Текущее долгоживущее состояние экрана.
     */
    public val state: StateFlow<S>

    /**
     * Поток одноразовых побочных эффектов для UI (навигация, тосты, диалоги).
     */
    public val effects: Flow<E>

    /**
     * Входная точка событий от UI/внешних источников.
     *
     * @param intent намерение изменить состояние/выполнить действие.
     */
    public fun accept(intent: I)
}
