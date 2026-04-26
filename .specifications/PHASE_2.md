# Phase 2 — Data Layer: Models & Room Database

## Goal

Define the full data model and compile-verified Room schema. After this phase, the project
compiles cleanly with Room KSP annotation processing, all domain types exist, and the database
abstraction is ready for the repository layer (Phase 3).

## Current State

- Phase 1 complete: Room 2.8.4, KSP 2.3.7, DataStore 1.2.1, Navigation 3 1.1.1, and
  kotlinx-serialization-json 1.11.0 are all wired into the build.
- `DrinkWatchApplication.kt` is a stub with no properties.
- No source files exist under `data/`.

## Package Structure Created in This Phase

```
com.example.drinkwatch/
  data/
    model/
      Session.kt
      Player.kt
      Drink.kt              ← DrinkType enum lives here
      GlassGroup.kt
      Event.kt              ← sealed class hierarchy
      DerivedState.kt       ← PlayerDerivedState, TakenGlass
    db/
      AppDatabase.kt
      entity/
        SessionEntity.kt
        PlayerEntity.kt
        DrinkEntity.kt
        GlassGroupEntity.kt
        EventEntity.kt
      dao/
        SessionDao.kt
        PlayerDao.kt
        DrinkDao.kt
        GlassGroupDao.kt
        EventDao.kt
```

---

## Task 1 — Domain Models (`data/model/`)

Pure Kotlin data classes — no Room annotations.

### `Session.kt`

```kotlin
package com.example.drinkwatch.data.model

data class Session(val id: Long, val name: String)
```

### `Player.kt`

```kotlin
package com.example.drinkwatch.data.model

data class Player(
    val id: Long,
    val sessionId: Long,
    val name: String,
    val phone: String,
    val note: String,
    val isDisabled: Boolean,
)
```

### `Drink.kt`

```kotlin
package com.example.drinkwatch.data.model

enum class DrinkType { SHOT, LONG_DRINK, NON_ALCOHOLIC }

data class Drink(
    val id: Long,
    val sessionId: Long,
    val name: String,
    val type: DrinkType,
    val isDisabled: Boolean,
)
```

### `GlassGroup.kt`

```kotlin
package com.example.drinkwatch.data.model

data class GlassGroup(val id: Long, val sessionId: Long, val letter: Char)
```

### `Event.kt`

```kotlin
package com.example.drinkwatch.data.model

sealed class Event {
    abstract val id: Long
    abstract val sessionId: Long
    abstract val timestampMs: Long

    data class Order(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
        val drinkId: Long,
        val glassGroup: Char?,
        val glassNumber: Int?,
    ) : Event()

    data class Return(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val glassGroup: Char,
        val glassNumber: Int,
    ) : Event()

    data class Timeout(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
        val durationSeconds: Int,   // 0 = cancel current timeout
    ) : Event()

    data class DisablePlayer(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
    ) : Event()

    data class DisableDrink(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val drinkId: Long,
    ) : Event()
}
```

### `DerivedState.kt`

Computed/derived types used by the ViewModel layer; produced by repositories in Phase 3.

```kotlin
package com.example.drinkwatch.data.model

data class PlayerDerivedState(
    val player: Player,
    val activeDrinkCount: Int,
    val isUnderTimeout: Boolean,
    val timeoutEndsAtMs: Long?,     // null when not under timeout
    val totalDrinks: Int,
    val totalTimeoutSeconds: Int,
    val unreturnedGlassCount: Int,
)

data class TakenGlass(
    val glassGroup: Char,
    val glassNumber: Int,
    val playerId: Long,
    val drinkId: Long,
    val takenAtMs: Long,
)
```

---

## Task 2 — Room Entities (`data/db/entity/`)

Each entity mirrors its domain model with Room annotations and includes
`.toDomain()` / `.toEntity()` extension functions.

### `SessionEntity.kt`

```kotlin
package com.example.drinkwatch.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.drinkwatch.data.model.Session

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

fun SessionEntity.toDomain() = Session(id = id, name = name)
fun Session.toEntity() = SessionEntity(id = id, name = name)
```

### `PlayerEntity.kt`

```kotlin
package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.Player

@Entity(
    tableName = "players",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val phone: String,
    val note: String,
    val isDisabled: Boolean = false,
)

fun PlayerEntity.toDomain() = Player(
    id = id, sessionId = sessionId, name = name,
    phone = phone, note = note, isDisabled = isDisabled,
)
fun Player.toEntity() = PlayerEntity(
    id = id, sessionId = sessionId, name = name,
    phone = phone, note = note, isDisabled = isDisabled,
)
```

### `DrinkEntity.kt`

```kotlin
package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType

@Entity(
    tableName = "drinks",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val type: DrinkType,
    val isDisabled: Boolean = false,
)

fun DrinkEntity.toDomain() = Drink(
    id = id, sessionId = sessionId, name = name,
    type = type, isDisabled = isDisabled,
)
fun Drink.toEntity() = DrinkEntity(
    id = id, sessionId = sessionId, name = name,
    type = type, isDisabled = isDisabled,
)
```

### `GlassGroupEntity.kt`

A unique composite index on `(sessionId, letter)` prevents duplicate group letters within a
session. This is essential because glass events identify groups by `Char`, not by row id.

```kotlin
package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.GlassGroup

@Entity(
    tableName = "glass_groups",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index(value = ["sessionId", "letter"], unique = true)],
)
data class GlassGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val letter: Char,
)

fun GlassGroupEntity.toDomain() = GlassGroup(id = id, sessionId = sessionId, letter = letter)
fun GlassGroup.toEntity() = GlassGroupEntity(id = id, sessionId = sessionId, letter = letter)
```

### `EventEntity.kt`

Single flat table; `type` discriminates which fields are populated.
A foreign key to `sessions` with CASCADE delete ensures events are removed when a session is deleted.

| Column            | Used by                          |
|-------------------|----------------------------------|
| `playerId`        | Order, Timeout, DisablePlayer    |
| `drinkId`         | Order, DisableDrink              |
| `glassGroup`      | Order (optional), Return         |
| `glassNumber`     | Order (optional), Return         |
| `durationSeconds` | Timeout                          |

```kotlin
package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.Event

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index("playerId")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampMs: Long,
    val type: String,
    val playerId: Long? = null,
    val drinkId: Long? = null,
    val glassGroup: Char? = null,
    val glassNumber: Int? = null,
    val durationSeconds: Int? = null,
) {
    companion object {
        const val TYPE_ORDER = "ORDER"
        const val TYPE_RETURN = "RETURN"
        const val TYPE_TIMEOUT = "TIMEOUT"
        const val TYPE_DISABLE_PLAYER = "DISABLE_PLAYER"
        const val TYPE_DISABLE_DRINK = "DISABLE_DRINK"
    }
}

fun EventEntity.toDomain(): Event = when (type) {
    EventEntity.TYPE_ORDER -> Event.Order(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
        drinkId = requireNotNull(drinkId),
        glassGroup = glassGroup,
        glassNumber = glassNumber,
    )
    EventEntity.TYPE_RETURN -> Event.Return(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        glassGroup = requireNotNull(glassGroup),
        glassNumber = requireNotNull(glassNumber),
    )
    EventEntity.TYPE_TIMEOUT -> Event.Timeout(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
        durationSeconds = requireNotNull(durationSeconds),
    )
    EventEntity.TYPE_DISABLE_PLAYER -> Event.DisablePlayer(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
    )
    EventEntity.TYPE_DISABLE_DRINK -> Event.DisableDrink(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        drinkId = requireNotNull(drinkId),
    )
    else -> error("Unknown event type: $type")
}

fun Event.toEntity(): EventEntity = when (this) {
    is Event.Order -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_ORDER,
        playerId = playerId, drinkId = drinkId,
        glassGroup = glassGroup, glassNumber = glassNumber,
    )
    is Event.Return -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_RETURN,
        glassGroup = glassGroup, glassNumber = glassNumber,
    )
    is Event.Timeout -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_TIMEOUT,
        playerId = playerId, durationSeconds = durationSeconds,
    )
    is Event.DisablePlayer -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_DISABLE_PLAYER,
        playerId = playerId,
    )
    is Event.DisableDrink -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_DISABLE_DRINK,
        drinkId = drinkId,
    )
}
```

---

## Task 3 — Room DAOs (`data/db/dao/`)

### `SessionDao.kt`

```kotlin
package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeById(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
```

### `PlayerDao.kt`

```kotlin
package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE sessionId = :sessionId ORDER BY name ASC")
    fun getAllBySession(sessionId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :id")
    fun observeById(id: Long): Flow<PlayerEntity?>

    @Query("UPDATE players SET isDisabled = :disabled WHERE id = :playerId")
    suspend fun setDisabled(playerId: Long, disabled: Boolean)
}
```

### `DrinkDao.kt`

```kotlin
package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.DrinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(drink: DrinkEntity): Long

    @Update
    suspend fun update(drink: DrinkEntity)

    @Delete
    suspend fun delete(drink: DrinkEntity)

    @Query("SELECT * FROM drinks WHERE sessionId = :sessionId ORDER BY name ASC")
    fun getAllBySession(sessionId: Long): Flow<List<DrinkEntity>>

    @Query("SELECT * FROM drinks WHERE id = :id")
    suspend fun getById(id: Long): DrinkEntity?

    @Query("SELECT * FROM drinks WHERE id = :id")
    fun observeById(id: Long): Flow<DrinkEntity?>

    @Query("UPDATE drinks SET isDisabled = :disabled WHERE id = :drinkId")
    suspend fun setDisabled(drinkId: Long, disabled: Boolean)
}
```

### `GlassGroupDao.kt`

```kotlin
package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.GlassGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlassGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(glassGroup: GlassGroupEntity): Long

    @Delete
    suspend fun delete(glassGroup: GlassGroupEntity)

    @Query("SELECT * FROM glass_groups WHERE sessionId = :sessionId ORDER BY letter ASC")
    fun getAllBySession(sessionId: Long): Flow<List<GlassGroupEntity>>
}
```

### `EventDao.kt`

```kotlin
package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getBySession(sessionId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE sessionId = :sessionId AND playerId = :playerId
        ORDER BY timestampMs ASC
    """)
    fun getByPlayer(sessionId: Long, playerId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE sessionId = :sessionId AND playerId = :playerId AND type = 'TIMEOUT'
        ORDER BY timestampMs DESC LIMIT 1
    """)
    fun getLatestTimeoutForPlayer(sessionId: Long, playerId: Long): Flow<EventEntity?>

    @Query("""
        SELECT * FROM events
        WHERE sessionId = :sessionId AND playerId = :playerId AND type = 'ORDER'
        AND timestampMs > COALESCE(
            (SELECT MAX(timestampMs) FROM events
             WHERE sessionId = :sessionId AND playerId = :playerId AND type = 'TIMEOUT'),
            0
        )
        ORDER BY timestampMs ASC
    """)
    fun getOrdersSinceLastTimeout(sessionId: Long, playerId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events AS o
        WHERE o.type = 'ORDER'
        AND o.glassGroup IS NOT NULL
        AND o.sessionId = :sessionId
        AND o.id = (
            SELECT MAX(e.id) FROM events AS e
            WHERE (e.type = 'ORDER' OR e.type = 'RETURN')
            AND e.glassGroup = o.glassGroup
            AND e.glassNumber = o.glassNumber
            AND e.sessionId = :sessionId
        )
    """)
    fun getTakenGlasses(sessionId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM events AS o
            WHERE o.type = 'ORDER'
            AND o.glassGroup = :glassGroup
            AND o.glassNumber = :glassNumber
            AND o.sessionId = :sessionId
            AND o.id = (
                SELECT MAX(e.id) FROM events AS e
                WHERE (e.type = 'ORDER' OR e.type = 'RETURN')
                AND e.glassGroup = :glassGroup
                AND e.glassNumber = :glassNumber
                AND e.sessionId = :sessionId
            )
        )
    """)
    suspend fun isGlassTaken(sessionId: Long, glassGroup: Char, glassNumber: Int): Boolean
}
```

**`getTakenGlasses` query rationale:** For each `(glassGroup, glassNumber)` pair the query finds
the ORDER event whose `id` equals the maximum `id` across all ORDER and RETURN events for that
pair. Because `id` is auto-incremented, the maximum id is the most recent event. If a RETURN was
the most recent event, no ORDER event will match, so the glass is excluded. Only glasses whose
most recent event is an ORDER appear in the results.

**Ordering invariant:** Glass-state queries use `MAX(id)` (not `MAX(timestampMs)`) as the
ordering key. This is correct for real-time recording where events are always appended. During
JSON import/export (Phase 3) event rows must be inserted in chronological order so that `id`
order matches timestamp order.

**`glassGroup` in entities is `String`/`String?`, not `Char`/`Char?`:** Room KSP generates
deprecated `toChar()` calls with Kotlin 2.x when `Char` is used directly in entity fields.
To avoid this, `GlassGroupEntity.letter` and `EventEntity.glassGroup` are stored as `String`.
The conversion between `Char` (domain) and `String` (entity) happens in the mapper functions
(`letter.first()` / `letter.toString()`). The `Char` ↔ `String` TypeConverter is therefore
not needed and not registered.

**`glassGroup` parameter type for `isGlassTaken`:** `String` (single char) is used to match
the stored column type directly.

---

## Task 4 — `AppDatabase` (`data/db/AppDatabase.kt`)

```kotlin
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
```

---

## Task 5 — Update `DrinkWatchApplication.kt`

Add lazy database initialization. Repositories will be added as `val` properties in Phase 3.

```kotlin
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
```

---

## Task 6 — Verify Build

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings. Room KSP will generate
`AppDatabase_Impl` and companion DAO implementations; a successful `assembleDebug` confirms
annotation processing is working.

---

## Task 7 — Update `README.md`

Change Phase 2 status from ⬜ to ✅ and add a summary of new files.
