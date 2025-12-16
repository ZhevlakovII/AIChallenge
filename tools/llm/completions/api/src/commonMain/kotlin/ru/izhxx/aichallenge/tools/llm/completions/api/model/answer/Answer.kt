package ru.izhxx.aichallenge.tools.llm.completions.api.model.answer

import ru.izhxx.aichallenge.tools.llm.completions.api.model.Usage

// TODO(заполнить документацию)
class Answer(
    val id: String,
    val createdAt: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)