package com.example.drinkwatch.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.drinkwatch.data.model.AppSettings
import com.example.drinkwatch.data.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private val defaults = AppSettings()

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACTIVE_DRINK_HIGHLIGHT = intPreferencesKey("active_drink_highlight")
        val DEFAULT_TIMEOUT_SECONDS = intPreferencesKey("default_timeout_seconds")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { Theme.valueOf(it) }.getOrNull() }
                ?: defaults.theme,
            activeDrinkHighlight = prefs[Keys.ACTIVE_DRINK_HIGHLIGHT]
                ?: defaults.activeDrinkHighlight,
            defaultTimeoutSeconds = prefs[Keys.DEFAULT_TIMEOUT_SECONDS]
                ?: defaults.defaultTimeoutSeconds,
        )
    }

    suspend fun setTheme(theme: Theme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setActiveDrinkHighlight(count: Int) {
        dataStore.edit { it[Keys.ACTIVE_DRINK_HIGHLIGHT] = count }
    }

    suspend fun setDefaultTimeoutSeconds(seconds: Int) {
        dataStore.edit { it[Keys.DEFAULT_TIMEOUT_SECONDS] = seconds }
    }
}
