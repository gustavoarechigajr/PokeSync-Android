package com.pokesync.android.ui.screens.saves

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokesync.android.data.local.DetectedSaveFile
import com.pokesync.android.data.local.EmulatorScanner
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.data.local.SaveRegistry
import com.pokesync.android.data.repository.PokemonRepository
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

data class SavesUiState(
    /** Per-save refresh state: saveId -> loading */
    val refreshing: Set<String> = emptySet(),
    val refreshError: Map<String, String> = emptyMap(),
    val isRefreshingAll: Boolean = false,
    val isScanning: Boolean = false,
    val detectedFiles: List<DetectedSaveFile> = emptyList(),
    val showAddSheet: Boolean = false,
    val globalError: String? = null,
)

@HiltViewModel
class SavesViewModel @Inject constructor(
    private val registry: SaveRegistry,
    private val repository: PokemonRepository,
    private val saveFileRepository: SaveFileRepository,
    private val scanner: EmulatorScanner,
) : ViewModel() {

    val saves: StateFlow<List<RegisteredSave>> = registry.saves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(SavesUiState())
    val ui: StateFlow<SavesUiState> = _ui

    // ── Refresh ───────────────────────────────────────────────────────────────

    /** Re-upload a single registered save; returns the new saveId for navigation. */
    fun refresh(save: RegisteredSave, onReady: (saveId: String) -> Unit) {
        if (save.id in _ui.value.refreshing) return
        _ui.update { it.copy(refreshing = it.refreshing + save.id) }
        viewModelScope.launch {
            runCatching { repository.uploadSaveFromPath(save.absolutePath) }
                .onSuccess { result ->
                    registry.update(
                        save.copy(
                            lastRefreshedAt = System.currentTimeMillis(),
                            lastSaveId = result.saveId,
                            gameVersion = result.gameVersion,
                            generation = result.generation,
                            trainerName = result.trainerName,
                        )
                    )
                    _ui.update { it.copy(refreshing = it.refreshing - save.id) }
                    onReady(result.saveId)
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            refreshing = it.refreshing - save.id,
                            refreshError = it.refreshError + (save.id to (e.message ?: "Upload failed")),
                        )
                    }
                }
        }
    }

    /** Refresh all registered saves in parallel. */
    fun refreshAll() {
        _ui.update { it.copy(isRefreshingAll = true, globalError = null) }
        viewModelScope.launch {
            val current = saves.value
            val jobs = current.map { save ->
                async {
                    runCatching { repository.uploadSaveFromPath(save.absolutePath) }
                        .onSuccess { result ->
                            registry.update(
                                save.copy(
                                    lastRefreshedAt = System.currentTimeMillis(),
                                    lastSaveId = result.saveId,
                                    gameVersion = result.gameVersion,
                                    generation = result.generation,
                                    trainerName = result.trainerName,
                                )
                            )
                        }
                }
            }
            jobs.forEach { it.await() }
            _ui.update { it.copy(isRefreshingAll = false) }
        }
    }

    // ── Add from file picker (SAF fallback) ──────────────────────────────────

    fun addFromUri(uri: Uri, emulatorHint: String = "Manual") {
        viewModelScope.launch {
            val saveFile = saveFileRepository.resolveSaveFile(uri)
            val path = saveFile?.uri?.path ?: return@launch
            runCatching { repository.uploadSaveFromUri(uri) }
                .onSuccess { result ->
                    registry.add(buildEntry(path, saveFile.displayName, emulatorHint, result))
                }
                .onFailure { e ->
                    _ui.update { it.copy(globalError = e.message) }
                }
        }
    }

    // ── Add from emulator scanner ─────────────────────────────────────────────

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
                    registry.add(
                        buildEntry(
                            path = detected.file.absolutePath,
                            fallbackName = detected.file.nameWithoutExtension,
                            emulator = detected.emulator,
                            result = result,
                        )
                    )
                }
                .onFailure { e ->
                    _ui.update { it.copy(globalError = "Not a Pokémon save: ${e.message}") }
                }
        }
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    fun remove(id: String) {
        viewModelScope.launch { registry.remove(id) }
    }

    fun clearError(saveId: String) =
        _ui.update { it.copy(refreshError = it.refreshError - saveId) }

    fun clearGlobalError() = _ui.update { it.copy(globalError = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildEntry(
        path: String,
        fallbackName: String,
        emulator: String,
        result: com.pokesync.android.data.repository.UploadResult,
    ) = RegisteredSave(
        id = UUID.randomUUID().toString(),
        absolutePath = path,
        displayName = if (result.trainerName.isNotBlank() && result.gameVersion.isNotBlank())
            "${result.gameVersion} — ${result.trainerName}"
        else fallbackName,
        emulator = emulator,
        gameVersion = result.gameVersion,
        generation = result.generation,
        trainerName = result.trainerName,
        addedAt = System.currentTimeMillis(),
        lastRefreshedAt = System.currentTimeMillis(),
        lastSaveId = result.saveId,
    )
}
