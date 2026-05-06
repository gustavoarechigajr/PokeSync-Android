package com.pokesync.android.ui.screens.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pokesync.android.domain.model.Pokemon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    saveId: String,
    onBack: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.transferComplete) {
        if (state.transferComplete) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer to Vault") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.selected.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::transferToVault,
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("Transfer ${state.selected.size}") },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading || state.isTransferring -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(state.error!!, color = MaterialTheme.colorScheme.error) }

            else -> LazyColumn(contentPadding = padding) {
                items(state.savePokemon, key = { it.id }) { pokemon ->
                    PokemonSelectRow(
                        pokemon = pokemon,
                        checked = pokemon.id in state.selected,
                        onToggle = { viewModel.toggleSelection(pokemon.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PokemonSelectRow(pokemon: Pokemon, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${pokemon.species}  Lv.${pokemon.level}${if (pokemon.isShiny) " ✦" else ""}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
