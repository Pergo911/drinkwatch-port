package com.example.drinkwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType
import com.example.drinkwatch.data.model.GlassGroup
import com.example.drinkwatch.data.model.Player
import com.example.drinkwatch.data.model.Session
import com.example.drinkwatch.data.repository.SessionRepository
import com.example.drinkwatch.data.serialization.SessionSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(
    private val sessionRepository: SessionRepository,
    private val sessionSerializer: SessionSerializer,
) : ViewModel() {

    // ── Exposed state ─────────────────────────────────────────────────────────

    val currentSession: StateFlow<Session?> =
        sessionRepository.observeCurrentSession()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val players: StateFlow<List<Player>> =
        currentSession.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else sessionRepository.observePlayers(session.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val drinks: StateFlow<List<Drink>> =
        currentSession.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else sessionRepository.observeDrinks(session.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val glassGroups: StateFlow<List<GlassGroup>> =
        currentSession.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else sessionRepository.observeGlassGroups(session.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    // ── Session actions ───────────────────────────────────────────────────────

    fun createSession(name: String) {
        viewModelScope.launch {
            sessionRepository.createSession(name)
        }
    }

    fun updateSessionName(name: String) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.updateSessionName(sessionId, name)
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    fun addPlayer(name: String, phone: String, note: String) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.addPlayer(sessionId, name, phone, note)
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch { sessionRepository.updatePlayer(player) }
    }

    fun savePlayer(player: Player, name: String, phone: String, note: String, isDisabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.updatePlayer(player.copy(name = name, phone = phone, note = note))
            if (isDisabled != player.isDisabled) {
                val sessionId = currentSession.value?.id ?: return@launch
                if (isDisabled) sessionRepository.disablePlayer(sessionId, player.id)
                else sessionRepository.enablePlayer(sessionId, player.id)
            }
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch { sessionRepository.deletePlayer(player) }
    }

    fun disablePlayer(player: Player) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.disablePlayer(sessionId, player.id)
        }
    }

    fun enablePlayer(player: Player) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.enablePlayer(sessionId, player.id)
        }
    }

    // ── Drink actions ─────────────────────────────────────────────────────────

    fun addDrink(name: String, type: DrinkType) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.addDrink(sessionId, name, type)
        }
    }

    fun updateDrink(drink: Drink) {
        viewModelScope.launch { sessionRepository.updateDrink(drink) }
    }

    fun saveDrink(drink: Drink, name: String, type: DrinkType, isDisabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.updateDrink(drink.copy(name = name, type = type))
            if (isDisabled != drink.isDisabled) {
                val sessionId = currentSession.value?.id ?: return@launch
                if (isDisabled) sessionRepository.disableDrink(sessionId, drink.id)
                else sessionRepository.enableDrink(sessionId, drink.id)
            }
        }
    }

    fun deleteDrink(drink: Drink) {
        viewModelScope.launch { sessionRepository.deleteDrink(drink) }
    }

    fun disableDrink(drink: Drink) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.disableDrink(sessionId, drink.id)
        }
    }

    fun enableDrink(drink: Drink) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            sessionRepository.enableDrink(sessionId, drink.id)
        }
    }

    // ── Glass group actions ───────────────────────────────────────────────────

    fun toggleGlassGroup(letter: Char) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            val exists = glassGroups.value.any { it.letter == letter }
            if (exists) {
                sessionRepository.removeGlassGroup(sessionId, letter)
            } else {
                sessionRepository.addGlassGroup(sessionId, letter)
            }
        }
    }

    // ── Import / export ───────────────────────────────────────────────────────

    fun exportSession(outputStream: OutputStream) {
        viewModelScope.launch {
            val sessionId = currentSession.value?.id ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    sessionSerializer.export(sessionId, outputStream)
                }
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Export failed"
            }
        }
    }

    fun importSession(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sessionSerializer.import(inputStream)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SerializationException) {
                _uiError.value = "Import failed: invalid or corrupted file."
            } catch (e: Exception) {
                _uiError.value = "Import failed."
            }
        }
    }

    fun reportError(message: String) {
        _uiError.value = message
    }

    fun clearError() {
        _uiError.value = null
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val sessionRepository: SessionRepository,
        private val sessionSerializer: SessionSerializer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            SessionViewModel(sessionRepository, sessionSerializer) as T
    }
}
