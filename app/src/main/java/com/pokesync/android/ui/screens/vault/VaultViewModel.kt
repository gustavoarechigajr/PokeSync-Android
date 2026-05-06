package com.pokesync.android.ui.screens.vault

import androidx.lifecycle.ViewModel
import com.pokesync.android.data.local.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class VaultUiState(
    val username: String = "",
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val authStore: AuthStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState(username = authStore.username ?: ""))
    val uiState: StateFlow<VaultUiState> = _uiState
}
