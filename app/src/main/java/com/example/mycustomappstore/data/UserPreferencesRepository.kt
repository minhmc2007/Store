package com.example.mycustomappstore.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(context: Context) {
    private val appContext = context.applicationContext

    val isFirstLaunch: Flow<Boolean> = appContext.dataStore.data
        .map { preferences ->
            preferences[IS_FIRST_LAUNCH_KEY] ?: true
        }

    suspend fun setFirstLaunchCompleted() {
        appContext.dataStore.edit { settings ->
            settings[IS_FIRST_LAUNCH_KEY] = false
        }
    }

    companion object {
        private val IS_FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")
    }
}
