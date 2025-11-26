package ru.izhxx.aichallenge.core.logger


actual object DefaultThreadInfoProvider : ThreadInfoProvider {
    override fun currentName(): String? = null
    // Надёжного идентификатора потока на iOS без дополнительных C interop нет — возвращаем null.
    override fun currentId(): Long? = null
}
