package com.pokesync.android.ui.screens.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokesync.android.data.repository.PokemonRepository
import com.pokesync.android.domain.model.Pokemon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferUiState(
    val savePokemon: List<Pokemon> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PokemonRepository,
) : ViewModel() {

    private val saveId: String = checkNotNull(savedStateHandle["saveId"])

    private val _uiState = MutableStateFlow(TransferUiState(isLoading = true))
    val uiState: StateFlow<TransferUiState> = _uiState

    init {
        loadSavePokemon()
    }

    private fun loadSavePokemon() {
        viewModelScope.launch {
            runCatching { repository.getSavePokemon(saveId) }
                .onSuccess { _uiState.value = TransferUiState(savePokemon = it) }
                .onFailure { _uiState.value = TransferUiState(error = it.message) }
        }
    }

    fun toggleSelection(pokemonId: String) {
        val current = _uiState.value.selected
        _uiState.value = _uiState.value.copy(
            selected = if (pokemonId in current) current - pokemonId else current + pokemonId,
        )
    }
}
