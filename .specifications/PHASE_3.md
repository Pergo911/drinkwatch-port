# Phase 3 — Repositories & Business Logic

## Goal

Expose reactive data and mutations to the ViewModel layer via `SessionRepository` and
`SettingsRepository`; implement JSON import/export via `SessionSerializer`; write unit tests for
all derived-state logic.

## Current state

- All domain models exist under `data/model/`.
- All Room entities, DAOs, and `AppDatabase` exist under `data/db/`.
- `DrinkWatchApplication` has a lazy `AppDatabase` instance and a stub comment for repositories.
- No repositories, serializer, or tests exist yet.

---

## New files

| Path | Description |
|---|---|
| `data/model/QueuedOrder.kt` | Shared queue-item data class |
| `data/model/AppSettings.kt` | Settings data class + `Theme` enum |
| `data/repository/SessionRepository.kt` | All session, player, drink, glass group, event ops |
| `data/repository/SettingsRepository.kt` | DataStore-backed settings |
| `data/serialization/SessionSnapshot.kt` | `@Serializable` flat transfer objects |
| `data/serialization/SessionSerializer.kt` | JSON export/import logic |
| `test/…/DerivedStateTest.kt` | Pure-Kotlin unit tests for derived-state logic |

## Modified files

| Path | Change |
|---|---|
| `data/db/dao/SessionDao.kt` | Add `suspend fun exists(): Boolean` |
| `data/db/dao/EventDao.kt` | Fix `getBySession` sort order to `(timestampMs, id)` |
| `DrinkWatchApplication.kt` | Wire `SessionRepository`, `SettingsRepository`, `SessionSerializer` as singletons |

---

## Tasks

### Task 1 — `QueuedOrder` model

Create `data/model/QueuedOrder.kt`:

```kotlin
data class QueuedOrder(
    val playerId: Long,
    val drinkId: Long,
    val glassGroup: Char?,
    val glassNumber: Int?,
)
```

---

### Task 2 — `AppSettings` model

Create `data/model/AppSettings.kt`:

```kotlin
enum class Theme { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: Theme = Theme.SYSTEM,
    val activeDrinkHighlight: Int = 3,
    val defaultTimeoutSeconds: Int = 300,
)
```

---

### Task 3 — Update `SessionDao`

Add an `EXISTS` query for cheap active-session detection:

```kotlin
@Query("SELECT COUNT(*) > 0 FROM sessions")
suspend fun exists(): Boolean
```

---

### Task 4 — Fix `EventDao.getBySession` sort order

Update the `getBySession` query to use canonical `(timestampMs, id)` ordering so
in-memory derived-state logic and SQL queries agree on which event is "latest":

```kotlin
@Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY timestampMs ASC, id ASC")
fun getBySession(sessionId: Long): Flow<List<EventEntity>>
```

---

### Task 5 — `SessionRepository`

Create `data/repository/SessionRepository.kt`.

#### Constructor

```kotlin
class SessionRepository(
    private val db: AppDatabase,
    private val sessionDao: SessionDao,
    private val playerDao: PlayerDao,
    private val drinkDao: DrinkDao,
    private val glassGroupDao: GlassGroupDao,
    private val eventDao: EventDao,
)
```

#### Session

```kotlin
// Enforces single-session invariant: deletes all existing data first.
suspend fun createSession(name: String): Long
suspend fun updateSessionName(sessionId: Long, name: String)
fun observeCurrentSession(): Flow<Session?>  // observes first (and only) session row
suspend fun hasActiveSession(): Boolean       // delegates to sessionDao.exists()
```

`createSession` must run inside `db.withTransaction {}`:
1. `sessionDao.deleteAll()` (cascades to all child tables).
2. Insert a new session with the given name; return the generated ID.

`observeCurrentSession` should observe `sessionDao.observeAll()` and map to the first element
(or null). If multi-session ever arises, only the first row is used.

#### Players

```kotlin
suspend fun addPlayer(sessionId: Long, name: String, phone: String, note: String): Long
suspend fun updatePlayer(player: Player)
suspend fun deletePlayer(player: Player)
suspend fun disablePlayer(sessionId: Long, playerId: Long, nowMs: Long = System.currentTimeMillis())
fun observePlayers(sessionId: Long): Flow<List<Player>>
```

`disablePlayer` inserts a `DisablePlayer` event and calls `playerDao.setDisabled(playerId, true)`.
These two operations run inside `db.withTransaction {}`.

#### Drinks

```kotlin
suspend fun addDrink(sessionId: Long, name: String, type: DrinkType): Long
suspend fun updateDrink(drink: Drink)
suspend fun deleteDrink(drink: Drink)
suspend fun disableDrink(sessionId: Long, drinkId: Long, nowMs: Long = System.currentTimeMillis())
fun observeDrinks(sessionId: Long): Flow<List<Drink>>
```

`disableDrink` mirrors `disablePlayer`: inserts a `DisableDrink` event and calls
`drinkDao.setDisabled(drinkId, true)` inside a transaction.

#### Glass Groups

```kotlin
suspend fun addGlassGroup(sessionId: Long, letter: Char): Long
suspend fun removeGlassGroup(sessionId: Long, letter: Char)
fun observeGlassGroups(sessionId: Long): Flow<List<GlassGroup>>
```

`removeGlassGroup` fetches the entity by `(sessionId, letter)` and deletes it. Add a helper
DAO query `getByLetter(sessionId, letter)` to `GlassGroupDao` if it doesn't already exist.

#### Derived state

```kotlin
fun observePlayerStates(sessionId: Long): Flow<List<PlayerDerivedState>>
fun observeTakenGlasses(sessionId: Long): Flow<List<TakenGlass>>
suspend fun isGlassTaken(sessionId: Long, group: Char, number: Int): Boolean
```

**`observePlayerStates`** uses a 3-way `combine()`:

```kotlin
fun observePlayerStates(sessionId: Long): Flow<List<PlayerDerivedState>> =
    combine(
        playerDao.getAllBySession(sessionId),
        drinkDao.getAllBySession(sessionId),
        eventDao.getBySession(sessionId),
    ) { players, drinks, events ->
        val drinkTypeById: Map<Long, DrinkType> = drinks.associate { it.id to it.type }
        val takenGlassesByKey: Map<Pair<String, Int>, Long> = computeTakenGlassKeys(events)
        players.map { e ->
            computePlayerDerivedState(
                player = e.toDomain(),
                events = events,
                drinkTypeById = drinkTypeById,
                takenGlassesByKey = takenGlassesByKey,
                nowMs = System.currentTimeMillis(),
            )
        }
    }
```

**`computeTakenGlassKeys`** (internal, top-level function — also used in tests):

```kotlin
// Returns Map<(glassGroup, glassNumber) -> playerId> for glasses that are currently taken.
// A glass is "taken" if the most-recent event (by id) for that (group, number) pair is an ORDER.
internal fun computeTakenGlassKeys(events: List<EventEntity>): Map<Pair<String, Int>, Long> {
    val relevant = events.filter {
        (it.type == TYPE_ORDER || it.type == TYPE_RETURN)
            && it.glassGroup != null && it.glassNumber != null
    }
    val latestByKey = mutableMapOf<Pair<String, Int>, EventEntity>()
    for (e in relevant) {
        val key = Pair(e.glassGroup!!, e.glassNumber!!)
        val existing = latestByKey[key]
        if (existing == null || e.id > existing.id) latestByKey[key] = e
    }
    return latestByKey
        .filter { (_, e) -> e.type == TYPE_ORDER }
        .mapValues { (_, e) -> e.playerId!! }
}
```

**`computePlayerDerivedState`** (internal, top-level function — also used in tests):

```kotlin
internal fun computePlayerDerivedState(
    player: Player,
    events: List<EventEntity>,
    drinkTypeById: Map<Long, DrinkType>,
    takenGlassesByKey: Map<Pair<String, Int>, Long>,
    nowMs: Long,
): PlayerDerivedState {
    val lastTimeout = events
        .filter { it.type == TYPE_TIMEOUT && it.playerId == player.id }
        .maxByOrNull { it.timestampMs * 1_000_000 + it.id }  // canonical order

    val isUnderTimeout = lastTimeout != null
        && lastTimeout.durationSeconds!! > 0
        && (lastTimeout.timestampMs + lastTimeout.durationSeconds * 1000L) > nowMs

    val timeoutEndsAtMs = if (isUnderTimeout)
        lastTimeout!!.timestampMs + lastTimeout.durationSeconds!! * 1000L
    else null

    val lastTimeoutTs = lastTimeout?.timestampMs ?: Long.MIN_VALUE

    val activeDrinkCount = events.count { e ->
        e.type == TYPE_ORDER
            && e.playerId == player.id
            && e.timestampMs > lastTimeoutTs
            && drinkTypeById[e.drinkId!!] != DrinkType.NON_ALCOHOLIC
    }

    val totalDrinks = events.count { e -> e.type == TYPE_ORDER && e.playerId == player.id }

    val totalTimeoutSeconds = events
        .filter { it.type == TYPE_TIMEOUT && it.playerId == player.id && (it.durationSeconds ?: 0) > 0 }
        .sumOf { it.durationSeconds!! }

    val unreturnedGlassCount = takenGlassesByKey.values.count { it == player.id }

    return PlayerDerivedState(
        player = player,
        activeDrinkCount = activeDrinkCount,
        isUnderTimeout = isUnderTimeout,
        timeoutEndsAtMs = timeoutEndsAtMs,
        totalDrinks = totalDrinks,
        totalTimeoutSeconds = totalTimeoutSeconds,
        unreturnedGlassCount = unreturnedGlassCount,
    )
}
```

**`observeTakenGlasses`** maps `eventDao.getTakenGlasses(sessionId)` to domain objects:

```kotlin
fun observeTakenGlasses(sessionId: Long): Flow<List<TakenGlass>> =
    eventDao.getTakenGlasses(sessionId).map { entities ->
        entities.map { e ->
            TakenGlass(
                glassGroup = e.glassGroup!!.first(),
                glassNumber = e.glassNumber!!,
                playerId = e.playerId!!,
                drinkId = e.drinkId!!,
                takenAtMs = e.timestampMs,
            )
        }
    }
```

#### Commit orders

```kotlin
suspend fun commitOrders(
    sessionId: Long,
    orders: List<QueuedOrder>,
    nowMs: Long = System.currentTimeMillis(),
)
```

Runs in `db.withTransaction {}`:
- Validate no duplicate `(glassGroup, glassNumber)` pairs within the batch (throw `IllegalArgumentException` on violation).
- For each `QueuedOrder`, insert a `Event.Order` entity.

No player/drink cross-session validation is required (the UI will only offer items from the current session).

#### Import session

```kotlin
suspend fun importSession(snapshot: SessionSnapshot)
```

Runs in `db.withTransaction {}`:
1. `sessionDao.deleteAll()` (cascades to all child tables).
2. Insert the new session → capture `newSessionId`.
3. Insert players; build `Map<Long, Long>` of `snapshotPlayerId → newPlayerId`.
4. Insert drinks; build `Map<Long, Long>` of `snapshotDrinkId → newDrinkId`.
5. Insert glass groups (letter is stable; no ID remap needed downstream).
6. Insert events, remapping `playerId` and `drinkId` via the maps built above. Skip any event whose
   referenced IDs are not present in the remap maps (defensive; prevents corrupt imports from
   crashing).
7. Restore `isDisabled` flags: replay `DISABLE_PLAYER` / `DISABLE_DRINK` events from the snapshot
   to update `PlayerEntity.isDisabled` / `DrinkEntity.isDisabled` (call `setDisabled(id, true)` for
   each), rather than trusting the snapshot's boolean field. This keeps the `isDisabled` column in
   sync with the event log.

---

### Task 6 — `SettingsRepository`

Create `data/repository/SettingsRepository.kt`:

```kotlin
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACTIVE_DRINK_HIGHLIGHT = intPreferencesKey("active_drink_highlight")
        val DEFAULT_TIMEOUT_SECONDS = intPreferencesKey("default_timeout_seconds")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[Keys.THEME]?.let { Theme.valueOf(it) } ?: Theme.SYSTEM,
            activeDrinkHighlight = prefs[Keys.ACTIVE_DRINK_HIGHLIGHT] ?: AppSettings().activeDrinkHighlight,
            defaultTimeoutSeconds = prefs[Keys.DEFAULT_TIMEOUT_SECONDS] ?: AppSettings().defaultTimeoutSeconds,
        )
    }

    suspend fun setTheme(theme: Theme) { dataStore.edit { it[Keys.THEME] = theme.name } }
    suspend fun setActiveDrinkHighlight(count: Int) { dataStore.edit { it[Keys.ACTIVE_DRINK_HIGHLIGHT] = count } }
    suspend fun setDefaultTimeoutSeconds(seconds: Int) { dataStore.edit { it[Keys.DEFAULT_TIMEOUT_SECONDS] = seconds } }
}
```

---

### Task 7 — `SessionSnapshot` & `SessionSerializer`

#### `data/serialization/SessionSnapshot.kt`

```kotlin
@Serializable
data class SessionSnapshot(
    val version: Int = 1,
    val name: String,
    val players: List<PlayerSnapshot>,
    val drinks: List<DrinkSnapshot>,
    val glassGroups: List<String>,           // list of single-char strings (e.g. ["A","B"])
    val events: List<EventSnapshot>,
)

@Serializable
data class PlayerSnapshot(
    val id: Long,
    val name: String,
    val phone: String,
    val note: String,
    val isDisabled: Boolean,
)

@Serializable
data class DrinkSnapshot(
    val id: Long,
    val name: String,
    val type: String,                        // DrinkType.name
    val isDisabled: Boolean,
)

@Serializable
data class EventSnapshot(
    val id: Long,
    val timestampMs: Long,
    val type: String,                        // EventEntity.TYPE_* constants
    val playerId: Long? = null,
    val drinkId: Long? = null,
    val glassGroup: String? = null,
    val glassNumber: Int? = null,
    val durationSeconds: Int? = null,
)
```

Note: snapshot IDs (`PlayerSnapshot.id`, `DrinkSnapshot.id`, `EventSnapshot.playerId`, etc.) are
the **original** IDs from the exporting device. `importSession` remaps them to new auto-generated
IDs on import. `EventSnapshot.id` is included for human readability / debugging but is not used
during import.

#### `data/serialization/SessionSerializer.kt`

```kotlin
class SessionSerializer(
    private val repository: SessionRepository,
    private val json: Json = Json { prettyPrint = false },
)
```

**`suspend fun export(sessionId: Long, outputStream: OutputStream)`:**

Collects all entity lists for `sessionId` (using `.first()` on their Flows), maps them to snapshot
DTOs, serialises to JSON, and writes UTF-8 bytes to `outputStream`.

**`suspend fun import(inputStream: InputStream): Unit`:**

Reads UTF-8 bytes, deserialises to `SessionSnapshot`, then calls `repository.importSession(snapshot)`.

---

### Task 8 — Wire singletons in `DrinkWatchApplication`

```kotlin
val sessionRepository: SessionRepository by lazy {
    SessionRepository(database, database.sessionDao(), database.playerDao(),
                      database.drinkDao(), database.glassGroupDao(), database.eventDao())
}

val settingsRepository: SettingsRepository by lazy {
    SettingsRepository(
        PreferenceDataStoreFactory.create { applicationContext.dataStoreFile("settings.preferences_pb") }
    )
}

val sessionSerializer: SessionSerializer by lazy {
    SessionSerializer(sessionRepository)
}
```

---

### Task 9 — Unit tests

Create `app/src/test/…/DerivedStateTest.kt` using JUnit 4.

Test `computePlayerDerivedState` and `computeTakenGlassKeys` as pure functions (no Room, no
Android dependencies). Use fabricated `EventEntity` lists.

**Mandatory test cases:**

| # | Test | Assertion |
|---|---|---|
| 1 | No events | `activeDrinkCount=0`, `isUnderTimeout=false`, `totalDrinks=0` |
| 2 | One alcoholic order, no timeout | `activeDrinkCount=1`, `totalDrinks=1` |
| 3 | One NON_ALCOHOLIC order | `activeDrinkCount=0`, `totalDrinks=1` |
| 4 | Two alcoholic orders then timeout (any duration) | `activeDrinkCount=0` after timeout |
| 5 | Timeout with `durationSeconds=0` (cancel) | `isUnderTimeout=false` |
| 6 | Active timeout | `isUnderTimeout=true`, `timeoutEndsAtMs` set correctly |
| 7 | Expired timeout | `isUnderTimeout=false`, `timeoutEndsAtMs=null` |
| 8 | Two events with same `timestampMs`, differ by `id` (latest id wins) | correct timeout/order chosen |
| 9 | Glass taken (ORDER with glass set) | `unreturnedGlassCount=1` in `takenGlassesByKey` |
| 10 | Glass taken then returned | `unreturnedGlassCount=0` |
| 11 | Same glass taken by two different orders (second replaces first) | only one entry |
| 12 | `totalTimeoutSeconds` is sum of non-zero timeout durations only |  |

---

## Verification

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings.
