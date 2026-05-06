package com.pokesync.android.ui.screens.saves

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pokesync.android.data.local.DetectedSaveFile
import com.pokesync.android.domain.model.RegisteredSave
import com.pokesync.android.ui.permission.RequestAllFilesAccessDialog
import com.pokesync.android.ui.permission.hasAllFilesAccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavesScreen(
    onBrowsePokemon: (saveId: String) -> Unit,
    viewModel: SavesViewModel = hiltViewModel(),
) {
    val saves by viewModel.saves.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showPermissionDialog by remember { mutableStateOf(!hasAllFilesAccess()) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addFromUri(it) }
    }

    // Show per-save errors in snackbar
    LaunchedEffect(ui.refreshError) {
        ui.refreshError.values.firstOrNull()?.let { msg ->
            snackbar.showSnackbar(msg)
        }
    }
    LaunchedEffect(ui.globalError) {
        ui.globalError?.let {
            snackbar.showSnackbar(it)
            viewModel.clearGlobalError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save Files") },
                actions = {
                    if (ui.isRefreshingAll) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(24.dp))
                    } else if (saves.isNotEmpty()) {
                        IconButton(onClick = viewModel::refreshAll) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh all")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openAddSheet) {
                Icon(Icons.Default.Add, contentDescription = "Add save")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (saves.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                onScanEmulators = viewModel::openAddSheet,
                onPickFile = { filePicker.launch(arrayOf("*/*")) },
            )
        } else {
            LazyColumn(contentPadding = padding) {
                items(saves, key = { it.id }) { save ->
                    SaveCard(
                        save = save,
                        isRefreshing = save.id in ui.refreshing,
                        error = ui.refreshError[save.id],
                        onRefresh = { viewModel.refresh(save, onBrowsePokemon) },
                        onBrowse = {
                            val id = save.lastSaveId
                            if (id != null) onBrowsePokemon(id)
                            else viewModel.refresh(save, onBrowsePokemon)
                        },
                        onRemove = { viewModel.remove(save.id) },
                        onClearError = { viewModel.clearError(save.id) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showPermissionDialog) {
        RequestAllFilesAccessDialog(onDismiss = { showPermissionDialog = false })
    }

    if (ui.showAddSheet) {
        AddSaveSheet(
            isScanning = ui.isScanning,
            detected = ui.detectedFiles,
            onSelect = viewModel::addDetected,
            onPickManually = { filePicker.launch(arrayOf("*/*")) },
            onDismiss = viewModel::closeAddSheet,
        )
    }
}

// ── Save card ─────────────────────────────────────────────────────────────────

@Composable
private fun SaveCard(
    save: RegisteredSave,
    isRefreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onBrowse: () -> Unit,
    onRemove: () -> Unit,
    onClearError: () -> Unit,
) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(save.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = buildString {
                            append(save.emulator)
                            save.generation?.let { append(" • Gen $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    save.lastRefreshedAt?.let { ts ->
                        Text(
                            "Last synced ${fmt.format(Date(ts))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync")
                    }
                }
                Button(
                    onClick = onBrowse,
                    enabled = !isRefreshing,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Browse")
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier, onScanEmulators: () -> Unit, onPickFile: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("No save files yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add a Pokémon save once and PokeSync will remember it.\nTap Sync any time to pull the latest version from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onScanEmulators, modifier = Modifier.fillMaxWidth()) {
                Text("Scan Emulators")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
                Text("Pick File Manually")
            }
        }
    }
}

// ── Add save bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSaveSheet(
    isScanning: Boolean,
    detected: List<DetectedSaveFile>,
    onSelect: (DetectedSaveFile) -> Unit,
    onPickManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Add Save File", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when {
                isScanning -> {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Scanning emulators…")
                        }
                    }
                }
                detected.isEmpty() -> {
                    Text(
                        "No save files found in known emulator directories.\nPick a file manually instead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val grouped = detected.groupBy { it.emulator }
                    grouped.forEach { (emulator, files) ->
                        Text(
                            emulator,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        files.forEach { file ->
                            ListItem(
                                headlineContent = { Text(file.file.nameWithoutExtension) },
                                supportingContent = {
                                    Text(
                                        file.file.parentFile?.name ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { onSelect(file); onDismiss() }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onPickManually(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pick File Manually")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
