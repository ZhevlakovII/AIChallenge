package ru.izhxx.aichallenge.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object DataStoreProvider : KoinComponent {
    actual fun providePreferencesDataStore(fileName: String): DataStore<Preferences> {
        val context: Context by inject()
        return PreferenceDataStoreFactory.create {
            context.dataStoreFile(fileName)
        }
    }
}