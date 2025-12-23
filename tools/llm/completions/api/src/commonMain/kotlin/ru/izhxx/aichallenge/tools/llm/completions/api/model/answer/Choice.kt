package ru.izhxx.aichallenge.tools.llm.completions.api.model.answer

/**
 * Вариант ответа от LLM (часть [Answer]).
 * LLM API может возвращать несколько вариантов ответа, каждый представлен отдельным [Choice].
 * В большинстве случаев используется первый элемент списка (index = 0).
 *
 * @property index Индекс варианта ответа в списке (обычно 0).
 * @property message Сгенерированное сообщение от LLM.
 *
 * @see Answer
 * @see AnswerMessage
 */
class Choice(
    val index: Int,
    val message: AnswerMessage
)