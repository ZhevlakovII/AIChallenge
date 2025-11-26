package ru.izhxx.aichallenge.core.logger

actual object DefaultThreadInfoProvider : ThreadInfoProvider {
    override fun currentName(): String? = Thread.currentThread().name
    override fun currentId(): Long? = Thread.currentThread().id
}
