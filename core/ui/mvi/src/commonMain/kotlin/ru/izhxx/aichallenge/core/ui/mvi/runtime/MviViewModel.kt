package ru.izhxx.aichallenge.core.ui.mvi.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.izhxx.aichallenge.core.ui.mvi.model.MviEffect
import ru.izhxx.aichallenge.core.ui.mvi.model.MviIntent
import ru.izhxx.aichallenge.core.ui.mvi.model.MviState

/**
 * Контракт MVI‑ViewModel для UI‑слоя (KMP, Compose Multiplatform, Android/iOS/Desktop).
 *
 * Назначение:
 * - Единая точка оркестрации MVI для конкретного экрана/фичи.
 * - Хранит текущее [state] как единственный источник правды.
 * - Принимает входящие [MviIntent] через [accept], запускает исполнение,
 *   преобразует результаты в новое [MviState], эмитит одноразовые [MviEffect] в [effects].
 *
 * Минимальный конвейер (без Action‑слоя):
 * UI → Intent → Executing → Result → Reduce(State × Result → State) → UI.
 *
 * Свойства:
 * - [state] — долгоживущий поток состояний для рендера (обычно наблюдается в Compose через collectAsState()).
 * - [effects] — одноразовые события (навигация, сообщения, диалоги), не хранятся в [MviState] и
 *   не должны воспроизводиться повторно при пересоздании подписки.
 *
 * Правила:
 * - [MviState] должен быть неизменяемым (immutability). Обновление состояния происходит только
 *   после обработки [MviIntent].
 * - [MviEffect] используйте для всего, что не должно быть частью долгоживущего состояния.
 * - [accept] должна быть потокобезопасной; реализация обычно опирается на корутины/Flow.
 *
 * Обязательства по использованию в Compose:
 * - Подписывайтесь на [state]: `val uiState by viewModel.state.collectAsStateWithLifecycle()`.
 * - Подписывайтесь на [effects] в `LaunchedEffect(viewModel)` и обрабатывайте каждое событие один раз.
 *
 * Типизация:
 * - Для каждого экрана определяются собственные sealed‑иерархии типов [S], [E], [I].
 *
 * Пример (схема):
 * ```
 * interface SearchViewModel : MviViewModel<SearchState, SearchEffect, SearchIntent>
 *
 * // В UI (Compose):
 * val state by viewModel.state.collectAsStateWithLifecycle()
 * LaunchedEffect(viewModel) {
 *   viewModel.effects.collect { effect ->
 *     when (effect) {
 *       is SearchEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
 *       is SearchEffect.NavigateToDetails -> navigator.openDetails(effect.id)
 *     }
 *   }
 * }
 * ```
 */
interface MviViewModel<S : MviState, E : MviEffect, I : MviIntent> {
    /**
     * Текущее долгоживущее состояние экрана.
     */
    val state: StateFlow<S>

    /**
     * Поток одноразовых побочных эффектов для UI (навигация, тосты, диалоги).
     */
    val effects: Flow<E>

    /**
     * Входная точка событий от UI/внешних источников.
     *
     * @param intent намерение изменить состояние/выполнить действие.
     */
    fun accept(intent: I)
}
