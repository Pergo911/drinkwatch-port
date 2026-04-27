package com.example.drinkwatch

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.drinkwatch.data.db.AppDatabase
import com.example.drinkwatch.data.repository.SessionRepository
import com.example.drinkwatch.data.repository.SettingsRepository
import com.example.drinkwatch.data.serialization.SessionSerializer
import com.example.drinkwatch.viewmodel.MainViewModel
import com.example.drinkwatch.viewmodel.PlayerDetailViewModel
import com.example.drinkwatch.viewmodel.SessionViewModel
import com.example.drinkwatch.viewmodel.SettingsViewModel

class DrinkWatchApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "drinkwatch.db",
        ).build()
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            db = database,
            sessionDao = database.sessionDao(),
            playerDao = database.playerDao(),
            drinkDao = database.drinkDao(),
            glassGroupDao = database.glassGroupDao(),
            eventDao = database.eventDao(),
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(dataStore = settingsDataStore)
    }

    val sessionSerializer: SessionSerializer by lazy {
        SessionSerializer(
            repository = sessionRepository,
            sessionDao = database.sessionDao(),
            playerDao = database.playerDao(),
            drinkDao = database.drinkDao(),
            glassGroupDao = database.glassGroupDao(),
            eventDao = database.eventDao(),
        )
    }

    val mainViewModelFactory: MainViewModel.Factory by lazy {
        MainViewModel.Factory(sessionRepository, settingsRepository)
    }

    val sessionViewModelFactory: SessionViewModel.Factory by lazy {
        SessionViewModel.Factory(sessionRepository, sessionSerializer)
    }

    val settingsViewModelFactory: SettingsViewModel.Factory by lazy {
        SettingsViewModel.Factory(settingsRepository)
    }

    fun playerDetailViewModelFactory(playerId: Long): PlayerDetailViewModel.Factory =
        PlayerDetailViewModel.Factory(sessionRepository, settingsRepository, playerId)
}

private val Application.settingsDataStore by preferencesDataStore(name = "settings")

