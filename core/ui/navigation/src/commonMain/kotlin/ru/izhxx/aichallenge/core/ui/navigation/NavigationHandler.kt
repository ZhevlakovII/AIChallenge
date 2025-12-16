package ru.izhxx.aichallenge.core.ui.navigation

interface NavigationHandler {

    fun handle(intent: NavigationIntent): Boolean
}
