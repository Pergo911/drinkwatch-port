package com.example.drinkwatch.data.db

import androidx.room.*
import com.example.drinkwatch.data.db.dao.*
import com.example.drinkwatch.data.db.entity.*
import com.example.drinkwatch.data.model.DrinkType

@Database(
    entities = [
        SessionEntity::class,
        PlayerEntity::class,
        DrinkEntity::class,
        GlassGroupEntity::class,
        EventEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun playerDao(): PlayerDao
    abstract fun drinkDao(): DrinkDao
    abstract fun glassGroupDao(): GlassGroupDao
    abstract fun eventDao(): EventDao

    class Converters {
        @TypeConverter fun drinkTypeToString(type: DrinkType): String = type.name
        @TypeConverter fun stringToDrinkType(string: String): DrinkType = DrinkType.valueOf(string)
    }
}
