package com.example.drinkwatch

import android.app.Application
import androidx.room.Room
import com.example.drinkwatch.data.db.AppDatabase

class DrinkWatchApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "drinkwatch.db",
        ).build()
    }

    // Singleton repositories will be added here in Phase 3.
}
