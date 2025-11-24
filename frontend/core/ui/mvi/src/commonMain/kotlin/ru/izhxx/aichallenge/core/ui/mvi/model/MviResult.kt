package ru.izhxx.aichallenge.core.ui.mvi.model

/**
 * Интерфейс‑маркер для результатов исполнения (Result) в MVI.
 *
 * Назначение:
 * - Result описывает итог обработки входящего [MviIntent] на уровне бизнес/интеракторной логики,
 *   который будет свернут (редуцирован) в новое [MviState] через редьюсер.
 * - Result является единственным входом для изменения [MviState] (через Reducer), тем самым
 *   обеспечивая предсказуемость и трассируемость изменений UI.
 *
 * Рекомендации по моделированию:
 * - Объявляйте Result как `sealed interface`/`sealed class` для экрана/фичи с исчерпывающим набором
 *   состояний результата: успех/частичный успех/прогресс/ошибка и т. п.
 * - Разделяйте Result и [MviEffect]: Result влияет на долгоживущее состояние (State),
 *   Effect — это одноразовое событие (навигация, тосты, SnackBar), которое не хранится в State.
 * - Включайте в Result только то, что нужно редьюсеру для вычисления нового State.
 *
 * Типичный поток данных:
 * Intent → Executor (выполнение) → Result → Reducer(State × Result → State) → UI.
 *
 * Пример:
 * ```
 * sealed interface SearchResult : MviResult {
 *   object Loading : SearchResult
 *   data class Data(val items: List<Item>) : SearchResult
 *   data class Error(val message: String) : SearchResult
 * }
 * ```
 */
public interface MviResult
