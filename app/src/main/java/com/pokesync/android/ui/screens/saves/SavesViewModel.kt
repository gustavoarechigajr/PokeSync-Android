package com.pokesync.android.ui.screens.saves

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.data.repository.PokemonRepository
import com.pokesync.android.domain.model.SaveFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavesUiState(
    val saves: List<SaveFile> = emptyList(),
    val uploadingSaveId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class SavesViewModel @Inject constructor(
    private val saveFileRepository: SaveFileRepository,
    private val pokemonRepository: PokemonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavesUiState())
    val uiState: StateFlow<SavesUiState> = _uiState

    fun onFilePicked(uri: Uri) {
        val save = saveFileRepository.resolveSaveFile(uri) ?: return
        _uiState.value = _uiState.value.copy(saves = _uiState.value.saves + save)
    }

    fun uploadSave(uri: Uri, onSuccess: (saveId: String) -> Unit) {
        _uiState.value = _uiState.value.copy(error = null)
        viewModelScope.launch {
            runCatching { pokemonRepository.uploadSave(uri) }
                .onSuccess { (saveId, _) ->
                    _uiState.value = _uiState.value.copy(uploadingSaveId = null)
                    onSuccess(saveId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(uploadingSaveId = null, error = e.message)
                }
        }
    }
}
