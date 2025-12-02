package ru.izhxx.aichallenge.core.ui.navigation

fun interface DestinationRoute {
    operator fun invoke(): String
}
