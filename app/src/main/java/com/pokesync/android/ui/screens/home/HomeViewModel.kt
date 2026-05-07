package com.pokesync.android.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokesync.android.data.local.AuthStore
import com.pokesync.android.data.local.DetectedSaveFile
import com.pokesync.android.data.local.EmulatorScanner
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.data.local.SaveRegistry
import com.pokesync.android.data.repository.PokemonRepository
import com.pokesync.android.data.repository.UploadResult
import com.pokesync.android.domain.model.Pokemon
import com.pokesync.android.domain.model.RegisteredSave
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class HomeTab { Save, Bank }

data class HomeUiState(
    // Save (left panel)
    val activeSave: RegisteredSave? = null,
    val savePokemon: List<Pokemon> = emptyList(),
    val saveBox: Int = 0,
    val saveBoxCount: Int = 1,
    val isSaveLoading: Boolean = false,
    // Bank (right panel)
    val bankPokemon: List<Pokemon> = emptyList(),
    val bankBox: Int = 0,
    val isBankLoading: Boolean = false,
    // Selection
    val selectedPokemon: Pokemon? = null,
    val selectedPokemonSource: HomeTab? = null,
    // Sheets
    val showAddSheet: Boolean = false,
    val isScanning: Boolean = false,
    val detectedFiles: List<DetectedSaveFile> = emptyList(),
    val globalError: String? = null,
)

val HomeUiState.bankBoxCount: Int
    get() = (bankPokemon.maxOfOrNull { it.box }?.plus(1))?.coerceAtLeast(1) ?: 1

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val registry: SaveRegistry,
    private val repository: PokemonRepository,
    private val saveFileRepository: SaveFileRepository,
    private val scanner: EmulatorScanner,
    val authStore: AuthStore,
) : ViewModel() {

    val saves: StateFlow<List<RegisteredSave>> = registry.saves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    init {
        // Auto-select first save when the list loads
        viewModelScope.launch {
            saves.collect { list ->
                if (_ui.value.activeSave == null && list.isNotEmpty()) {
                    loadSave(list.first())
                }
            }
        }
        loadBank()
    }

    // ── Save panel ────────────────────────────────────────────────────────────

    fun selectSave(save: RegisteredSave) {
        if (save.id == _ui.value.activeSave?.id) return
        loadSave(save)
    }

    private fun loadSave(save: RegisteredSave) {
        val saveId = save.lastSaveId ?: return
        _ui.update { it.copy(activeSave = save, isSaveLoading = true, savePokemon = emptyList(), saveBox = 0) }
        viewModelScope.launch {
            runCatching { repository.getSavePokemon(saveId) }
                .onSuccess { pkm ->
                    _ui.update {
                        it.copy(
                            isSaveLoading = false,
                            savePokemon = pkm,
                            saveBoxCount = pkm.maxOfOrNull { p -> p.box }?.plus(1) ?: save.boxCount ?: 1,
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isSaveLoading = false, globalError = e.message) }
                }
        }
    }

    fun prevSaveBox() = _ui.update { s -> s.copy(saveBox = (s.saveBox - 1).coerceAtLeast(0), selectedPokemon = null) }
    fun nextSaveBox() = _ui.update { s -> s.copy(saveBox = (s.saveBox + 1).coerceAtMost(s.saveBoxCount - 1), selectedPokemon = null) }

    fun syncActiveSave() {
        val save = _ui.value.activeSave ?: return
        _ui.update { it.copy(isSaveLoading = true) }
        viewModelScope.launch {
            runCatching { repository.uploadSaveFromPath(save.absolutePath) }
                .onSuccess { result ->
                    val updated = save.copy(
                        lastRefreshedAt = System.currentTimeMillis(),
                        lastSaveId = result.saveId,
                        gameVersion = result.gameVersion,
                        generation = result.generation,
                        trainerName = result.trainerName,
                    )
                    registry.update(updated)
                    _ui.update {
                        it.copy(
                            activeSave = updated,
                            isSaveLoading = false,
                            savePokemon = result.pokemon,
                            saveBoxCount = result.boxCount.coerceAtLeast(1),
                            saveBox = 0,
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isSaveLoading = false, globalError = e.message) }
                }
        }
    }

    // ── Bank panel ────────────────────────────────────────────────────────────

    fun loadBank() {
        _ui.update { it.copy(isBankLoading = true) }
        viewModelScope.launch {
            runCatching { repository.getVault() }
                .onSuccess { pkm -> _ui.update { it.copy(isBankLoading = false, bankPokemon = pkm) } }
                .onFailure { _ui.update { it.copy(isBankLoading = false) } }
        }
    }

    fun importSaveToBank(replace: Boolean = false) {
        val saveId = _ui.value.activeSave?.lastSaveId ?: return
        _ui.update { it.copy(isBankLoading = true) }
        viewModelScope.launch {
            runCatching { repository.importToVault(saveId, replace) }
                .onSuccess { pkm ->
                    _ui.update { it.copy(isBankLoading = false, bankPokemon = pkm) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isBankLoading = false, globalError = e.message) }
                }
        }
    }

    fun addPokemonToVault(pokemon: Pokemon) {
        val saveId = _ui.value.activeSave?.lastSaveId ?: return
        _ui.update { it.copy(isBankLoading = true) }
        viewModelScope.launch {
            runCatching { repository.addSingleToVault(saveId, pokemon.box, pokemon.slot) }
                .onSuccess { pkm ->
                    _ui.update { it.copy(isBankLoading = false, bankPokemon = pkm, selectedPokemon = null, selectedPokemonSource = null) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isBankLoading = false, globalError = e.message) }
                }
        }
    }

    fun removeFromBank(id: String) {
        viewModelScope.launch {
            runCatching { repository.removeFromVault(id) }
                .onSuccess {
                    _ui.update { s -> s.copy(bankPokemon = s.bankPokemon.filter { it.id != id }, selectedPokemon = null) }
                }
                .onFailure { e -> _ui.update { it.copy(globalError = e.message) } }
        }
    }

    fun prevBankBox() = _ui.update { s -> s.copy(bankBox = (s.bankBox - 1).coerceAtLeast(0), selectedPokemon = null) }
    fun nextBankBox() = _ui.update { s ->
        val max = s.bankBoxCount - 1
        s.copy(bankBox = (s.bankBox + 1).coerceAtMost(max), selectedPokemon = null)
    }

    // ── Pokemon selection ─────────────────────────────────────────────────────

    fun selectPokemon(pokemon: Pokemon?, source: HomeTab?) =
        _ui.update { it.copy(selectedPokemon = pokemon, selectedPokemonSource = source) }

    // ── Add save sheet ────────────────────────────────────────────────────────

    fun openAddSheet() {
        _ui.update { it.copy(showAddSheet = true, isScanning = true, detectedFiles = emptyList()) }
        viewModelScope.launch {
            val files = scanner.scanAll()
            _ui.update { it.copy(isScanning = false, detectedFiles = files) }
        }
    }

    fun closeAddSheet() = _ui.update { it.copy(showAddSheet = false) }

    fun addDetected(detected: DetectedSaveFile) {
        _ui.update { it.copy(showAddSheet = false) }
        viewModelScope.launch {
            runCatching { repository.uploadSaveFromPath(detected.file.absolutePath) }
                .onSuccess { result ->
                    val entry = buildEntry(
                        path = detected.file.absolutePath,
                        fallbackName = detected.gameName ?: detected.file.nameWithoutExtension,
                        emulator = detected.emulator,
                        result = result,
                    )
                    registry.add(entry)
                    loadSave(entry)
                }
                .onFailure { e ->
                    _ui.update { it.copy(globalError = "Not a Pokémon save: ${e.message}") }
                }
        }
    }

    fun addFromUri(uri: Uri, emulatorHint: String = "Manual") {
        viewModelScope.launch {
            val saveFile = saveFileRepository.resolveSaveFile(uri)
            val path = saveFile?.uri?.path ?: return@launch
            runCatching { repository.uploadSaveFromUri(uri) }
                .onSuccess { result ->
                    val entry = buildEntry(path, saveFile.displayName, emulatorHint, result)
                    registry.add(entry)
                    loadSave(entry)
                }
                .onFailure { e ->
                    _ui.update { it.copy(globalError = e.message) }
                }
        }
    }

    fun removeSave(id: String) {
        viewModelScope.launch {
            registry.remove(id)
            if (_ui.value.activeSave?.id == id) {
                val remaining = saves.value.filter { it.id != id }
                if (remaining.isNotEmpty()) loadSave(remaining.first())
                else _ui.update { it.copy(activeSave = null, savePokemon = emptyList(), saveBox = 0, saveBoxCount = 1) }
            }
        }
    }

    fun onVaultSlotDrop(pokemon: Pokemon, toBox: Int, toSlot: Int) {
        if (pokemon.box == toBox && pokemon.slot == toSlot) return
        viewModelScope.launch {
            runCatching { repository.moveVaultPokemon(pokemon.id, toBox, toSlot) }
                .onSuccess {
                    // Optimistic update
                    _ui.update { s ->
                        val updated = s.bankPokemon.map {
                            if (it.id == pokemon.id) it.copy(box = toBox, slot = toSlot) else it
                        }
                        s.copy(bankPokemon = updated)
                    }
                }
                .onFailure { loadBank() } // Re-sync on failure
        }
    }

    fun clearError() = _ui.update { it.copy(globalError = null) }

    private fun buildEntry(path: String, fallbackName: String, emulator: String, result: UploadResult) =
        RegisteredSave(
            id = UUID.randomUUID().toString(),
            absolutePath = path,
            displayName = if (result.trainerName.isNotBlank() && result.gameVersion.isNotBlank())
                "${result.gameVersion} — ${result.trainerName}"
            else fallbackName,
            emulator = emulator,
            gameVersion = result.gameVersion,
            generation = result.generation,
            trainerName = result.trainerName,
            boxCount = result.boxCount,
            addedAt = System.currentTimeMillis(),
            lastRefreshedAt = System.currentTimeMillis(),
            lastSaveId = result.saveId,
        )
}
