package ru.izhxx.aichallenge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform