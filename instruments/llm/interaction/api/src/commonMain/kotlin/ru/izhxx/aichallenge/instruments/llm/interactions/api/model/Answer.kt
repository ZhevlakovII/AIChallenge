package ru.izhxx.aichallenge.instruments.llm.interactions.api.model

// TODO(заполнить документацию)
class Answer(
    val id: String,
    val objectType: String,
    val createdAt: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)