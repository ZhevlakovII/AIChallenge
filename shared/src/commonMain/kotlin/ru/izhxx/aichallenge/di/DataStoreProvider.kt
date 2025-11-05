package ru.izhxx.aichallenge.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect object DataStoreProvider {
    fun providePreferencesDataStore(fileName: String): DataStore<Preferences>
}