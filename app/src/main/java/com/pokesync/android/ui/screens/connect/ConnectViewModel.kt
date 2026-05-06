package com.pokesync.android.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokesync.android.data.api.LoginRequest
import com.pokesync.android.data.api.PokeSyncApi
import com.pokesync.android.di.ServerUrlProvider
import com.pokesync.android.di.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val serverUrl: String = "http://",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val api: PokeSyncApi,
    private val tokenStore: TokenStore,
    private val serverUrlProvider: ServerUrlProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState

    fun onServerUrlChange(url: String) = _uiState.value.let { _uiState.value = it.copy(serverUrl = url) }
    fun onUsernameChange(v: String) = _uiState.value.let { _uiState.value = it.copy(username = v) }
    fun onPasswordChange(v: String) = _uiState.value.let { _uiState.value = it.copy(password = v) }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null)
        serverUrlProvider.url = state.serverUrl.trimEnd('/') + "/"
        viewModelScope.launch {
            runCatching { api.login(LoginRequest(state.username, state.password)) }
                .onSuccess { response ->
                    tokenStore.token = response.token
                    tokenStore.userId = response.userId
                    tokenStore.username = response.username
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }
}
