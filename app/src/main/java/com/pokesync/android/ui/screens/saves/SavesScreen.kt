package com.pokesync.android.ui.screens.saves

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
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
import com.pokesync.android.domain.model.SaveFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavesScreen(
    onTransfer: (saveId: String) -> Unit,
    viewModel: SavesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFilePicked(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Save Files") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Default.Add, contentDescription = "Pick save file")
            }
        },
    ) { padding ->
        if (state.saves.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Tap + to pick a save file from your device.")
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(state.saves) { save ->
                    SaveRow(
                        save = save,
                        onClick = { viewModel.uploadSave(save.uri, onTransfer) },
                    )
                }
            }
        }

        state.error?.let { error ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun SaveRow(save: SaveFile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(save.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = save.gameTitle ?: "Unknown game  •  ${save.sizeBytes / 1024} KB",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text("Tap to upload & browse Pokémon", style = MaterialTheme.typography.labelSmall)
        }
    }
}
