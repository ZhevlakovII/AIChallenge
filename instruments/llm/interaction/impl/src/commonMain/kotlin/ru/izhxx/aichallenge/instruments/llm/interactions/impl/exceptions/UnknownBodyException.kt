package ru.izhxx.aichallenge.instruments.llm.interactions.impl.exceptions

internal class UnknownBodyException(body: String?) : RuntimeException(
    message = "Unknown body, body ${body ?: "empty or null body"}"
)
