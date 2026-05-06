package com.pokesync.android.ui.screens.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pokesync.android.domain.model.Pokemon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onOpenSaves: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vault") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenSaves) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open saves")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.pokemon.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Pokémon in vault yet. Upload a save file to get started.")
            }
            else -> LazyColumn(contentPadding = padding) {
                items(state.pokemon) { pokemon ->
                    PokemonRow(pokemon)
                }
            }
        }
    }
}

@Composable
private fun PokemonRow(pokemon: Pokemon) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = pokemon.spriteUrl,
            contentDescription = pokemon.species,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = pokemon.nickname?.takeIf { it != pokemon.species } ?: pokemon.species,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = buildString {
                    append("Lv. ${pokemon.level}  •  ${pokemon.species}")
                    if (pokemon.isShiny) append("  ✦")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
