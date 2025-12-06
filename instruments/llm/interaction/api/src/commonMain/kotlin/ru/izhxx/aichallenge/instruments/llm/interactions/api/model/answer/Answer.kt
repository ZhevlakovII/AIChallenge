package ru.izhxx.aichallenge.instruments.llm.interactions.api.model.answer

import ru.izhxx.aichallenge.instruments.llm.interactions.api.model.Usage

// TODO(заполнить документацию)
class Answer(
    val id: String,
    val createdAt: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)