package ru.izhxx.aichallenge.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import org.koin.core.component.KoinComponent
import java.io.File
import java.nio.file.Paths

actual object DataStoreProvider : KoinComponent {
    actual fun providePreferencesDataStore(fileName: String): DataStore<Preferences> {
        val userHome = System.getProperty("user.home") ?: "."
        val dataStoreFile = File(userHome, ".aichallenge/preferences.preferences_pb")

        return PreferenceDataStoreFactory.create {
            Paths.get(dataStoreFile.absolutePath).toFile()
        }
    }
}