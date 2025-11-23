package ru.izhxx.aichallenge.core.mvi.runtime

import ru.izhxx.aichallenge.core.mvi.model.MviEffect
import ru.izhxx.aichallenge.core.mvi.model.MviIntent
import ru.izhxx.aichallenge.core.mvi.model.MviResult

/**
 * Исполнитель (Executor) — место выполнения побочных эффектов по входящему [MviIntent].
 *
 * Назначение:
 * - Принимает Intent из UI, выполняет необходимые операции (асинхронные/IO/бизнес‑логика),
 *   и эмитит:
 *   - [MviResult] — для свёртки редьюсером в новое состояние.
 *   - [MviEffect] — для одноразовых событий (навигация, тосты и т. п.).
 *
 * Роль в потоке данных:
 * UI → Intent → Executor (выполнение) → Result/Effect → Reducer(State × Result → State) → UI
 *
 * Правила и рекомендации:
 * - Executor — единственное место для сайд‑эффектов в MVI‑конвейере UI.
 * - Не изменяйте состояние UI напрямую; используйте [emitResult] для генерации [MviResult],
 *   который затем преобразуется редьюсером в новое состояние.
 * - Используйте [emitEffect], когда нужно отправить одноразовое событие, которое нельзя
 *   сохранять в состоянии (например, навигация/сообщение).
 * - Реализация, как правило, использует корутины/Flow; интерфейс остаётся платформенно‑независимым.
 *
 * Моделирование типов:
 * - Для каждого экрана определяются собственные sealed‑иерархии Intent/Result/Effect.
 * - Это обеспечивает типобезопасность и исчерпывающее покрытие сценариев.
 *
 * Пример использования (схематично):
 * ```
 * class SearchExecutor : MviExecutor<SearchIntent, SearchResult, SearchEffect> {
 *   override suspend fun execute(
 *     intent: SearchIntent,
 *     emitResult: suspend (SearchResult) -> Unit,
 *     emitEffect: suspend (SearchEffect) -> Unit
 *   ) {
 *     when (intent) {
 *       is SearchIntent.QueryChanged -> emitResult(SearchResult.QueryUpdated(intent.value))
 *       is SearchIntent.SubmitClicked -> {
 *         emitResult(SearchResult.Loading)
 *         try {
 *           val items = repository.search(currentQuery)
 *           emitResult(SearchResult.Data(items))
 *         } catch (t: Throwable) {
 *           emitResult(SearchResult.Error(t.message ?: "Unknown"))
 *           emitEffect(SearchEffect.ShowMessage("Ошибка загрузки"))
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 */
public interface MviExecutor<I : MviIntent, R : MviResult, E : MviEffect> {
    /**
     * Выполнить обработку входящего [intent].
     *
     * @param intent входящее намерение из UI.
     * @param emitResult коллбэк для эмиссии [MviResult], который будет свёрнут редьюсером в состояние.
     * @param emitEffect коллбэк для эмиссии одноразового [MviEffect] для UI.
     */
    public suspend fun execute(
        intent: I,
        emitResult: suspend (R) -> Unit,
        emitEffect: suspend (E) -> Unit
    )
}
