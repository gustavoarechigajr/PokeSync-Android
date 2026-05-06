package com.pokesync.android.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokesync.android.data.api.LoginRequest
import com.pokesync.android.data.api.PokeSyncApi
import com.pokesync.android.data.local.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val serverUrl: String = "http://pokesync.arechigawinpc.duckdns.org",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val api: PokeSyncApi,
    private val authStore: AuthStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState

    init {
        viewModelScope.launch {
            authStore.serverUrl.collect { url ->
                if (url != null) _uiState.value = _uiState.value.copy(serverUrl = url)
            }
        }
    }

    fun onServerUrlChange(url: String) { _uiState.value = _uiState.value.copy(serverUrl = url) }
    fun onUsernameChange(v: String) { _uiState.value = _uiState.value.copy(username = v) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(password = v) }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            authStore.saveServerUrl(state.serverUrl.trimEnd('/') + "/")

            runCatching { api.login(LoginRequest(state.username, state.password)) }
                .onSuccess { response ->
                    authStore.token = response.token
                    authStore.userId = response.userId
                    authStore.username = response.username
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            authStore.saveServerUrl(state.serverUrl.trimEnd('/') + "/")

            runCatching { api.register(LoginRequest(state.username, state.password)) }
                .onSuccess { response ->
                    authStore.token = response.token
                    authStore.userId = response.userId
                    authStore.username = response.username
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }
}
