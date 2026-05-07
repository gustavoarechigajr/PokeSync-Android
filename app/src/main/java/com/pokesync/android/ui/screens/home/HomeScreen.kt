package com.pokesync.android.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pokesync.android.data.local.DetectedSaveFile
import com.pokesync.android.domain.model.Pokemon
import com.pokesync.android.domain.model.RegisteredSave
import com.pokesync.android.ui.permission.RequestAllFilesAccessDialog
import com.pokesync.android.ui.permission.hasAllFilesAccess
import com.pokesync.android.ui.theme.HomeYellow
import kotlinx.coroutines.launch

private data class DragInfo(
    val pokemon: Pokemon,
    val source: HomeTab,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val saves by viewModel.saves.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showPermissionDialog by remember { mutableStateOf(!hasAllFilesAccess()) }

    // ── Drag state (purely UI, not in ViewModel) ──────────────────────────────
    var dragInfo by remember { mutableStateOf<DragInfo?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    // Vault tab bounds — for detecting "drop on vault tab" from save panel
    var vaultTabBounds by remember { mutableStateOf(Rect.Zero) }
    // Vault slot bounds — for detecting which slot to drop into within vault
    val vaultSlotBounds = remember { mutableStateMapOf<Int, Rect>() }

    val isDragging = dragInfo != null

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addFromUri(it) }
    }

    LaunchedEffect(ui.globalError) {
        ui.globalError?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    // Drag callbacks passed to both panels
    val onDragStart = { pokemon: Pokemon, source: HomeTab, startPos: Offset ->
        dragInfo = DragInfo(pokemon, source)
        dragPosition = startPos
    }
    val onDragMove = { delta: Offset -> dragPosition += delta }
    val onDragEnd = {
        val info = dragInfo
        val pos = dragPosition
        if (info != null) {
            when {
                // Save → Vault: drop on vault tab
                info.source == HomeTab.Save && vaultTabBounds.contains(pos) -> {
                    viewModel.importSaveToBank(replace = false)
                    viewModel.selectTab(HomeTab.Bank)
                }
                // Vault → Vault: rearrange within grid
                info.source == HomeTab.Bank -> {
                    val targetSlot = vaultSlotBounds.entries
                        .firstOrNull { (_, rect) -> rect.contains(pos) }
                        ?.key
                    if (targetSlot != null) {
                        viewModel.onVaultSlotDrop(info.pokemon, ui.bankBox, targetSlot)
                    }
                }
            }
        }
        dragInfo = null
    }
    val onDragCancel = { dragInfo = null }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SavesDrawer(
                    saves = saves,
                    activeSave = ui.activeSave,
                    onSelectSave = { viewModel.selectSave(it); scope.launch { drawerState.close() } },
                    onRemoveSave = viewModel::removeSave,
                    onAddSave = { scope.launch { drawerState.close() }; viewModel.openAddSheet() },
                )
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open saves")
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    ui.activeSave?.displayName ?: "PokeSync",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (ui.activeSave != null) {
                                    Text(
                                        ui.activeSave!!.emulator,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        },
                        actions = {
                            if (ui.isSaveLoading || ui.isBankLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 16.dp).size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else if (ui.selectedTab == HomeTab.Save && ui.activeSave != null) {
                                IconButton(onClick = viewModel::syncActiveSave) {
                                    Icon(Icons.Default.Refresh, "Sync save", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            } else if (ui.selectedTab == HomeTab.Bank) {
                                IconButton(onClick = viewModel::loadBank) {
                                    Icon(Icons.Default.Refresh, "Refresh vault", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = viewModel::openAddSheet,
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Add, "Add save", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    TabRow(
                        selectedTabIndex = ui.selectedTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Tab(
                            selected = ui.selectedTab == HomeTab.Save,
                            onClick = { viewModel.selectTab(HomeTab.Save) },
                            text = { Text(ui.activeSave?.gameVersion ?: "Save") },
                        )
                        // Vault tab — also acts as drop target when dragging from save
                        val vaultTabHighlight = isDragging && dragInfo?.source == HomeTab.Save
                        Tab(
                            selected = ui.selectedTab == HomeTab.Bank,
                            onClick = { viewModel.selectTab(HomeTab.Bank) },
                            modifier = Modifier
                                .onGloballyPositioned { vaultTabBounds = it.boundsInRoot() }
                                .then(
                                    if (vaultTabHighlight)
                                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                    else Modifier
                                ),
                            text = {
                                Text(if (vaultTabHighlight) "Drop here ▼" else "Vault")
                            },
                        )
                    }

                    when (ui.selectedTab) {
                        HomeTab.Save -> SavePanel(
                            pokemon = ui.savePokemon,
                            currentBox = ui.saveBox,
                            boxCount = ui.saveBoxCount,
                            selectedPokemon = ui.selectedPokemon,
                            isLoading = ui.isSaveLoading,
                            hasActiveSave = ui.activeSave != null,
                            isDragging = isDragging,
                            onPrev = viewModel::prevSaveBox,
                            onNext = viewModel::nextSaveBox,
                            onSelectPokemon = { viewModel.selectPokemon(it, HomeTab.Save) },
                            onAddSave = viewModel::openAddSheet,
                            onDragStart = { pkm, pos -> onDragStart(pkm, HomeTab.Save, pos) },
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                        HomeTab.Bank -> BankPanel(
                            pokemon = ui.bankPokemon,
                            currentBox = ui.bankBox,
                            boxCount = ui.bankBoxCount,
                            selectedPokemon = ui.selectedPokemon,
                            isLoading = ui.isBankLoading,
                            hasSaveLoaded = ui.activeSave != null,
                            isDragging = isDragging,
                            dragPosition = dragPosition,
                            slotBoundsMap = vaultSlotBounds,
                            onPrev = viewModel::prevBankBox,
                            onNext = viewModel::nextBankBox,
                            onSelectPokemon = { viewModel.selectPokemon(it, HomeTab.Bank) },
                            onImportSave = { viewModel.importSaveToBank(replace = false) },
                            onDragStart = { pkm, pos -> onDragStart(pkm, HomeTab.Bank, pos) },
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                }
            }
        }

        // ── Drag ghost overlay ─────────────────────────────────────────────────
        if (isDragging && dragInfo != null) {
            AsyncImage(
                model = dragInfo!!.pokemon.spriteUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .offset { IntOffset((dragPosition.x - 32.dp.toPx()).toInt(), (dragPosition.y - 32.dp.toPx()).toInt()) }
                    .alpha(0.85f)
                    .scale(1.2f),
            )
        }
    }

    // ── Pokémon detail sheet ──────────────────────────────────────────────────
    if (ui.selectedPokemon != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectPokemon(null, null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            PokemonDetailSheet(
                pokemon = ui.selectedPokemon!!,
                isBankPokemon = ui.selectedPokemonSource == HomeTab.Bank,
                onStoreToBankClick = { viewModel.storePokemonInBank(ui.selectedPokemon!!) },
                onRemoveFromBankClick = { viewModel.removeFromBank(ui.selectedPokemon!!.id) },
                onDismiss = { viewModel.selectPokemon(null, null) },
            )
        }
    }

    // ── Add save sheet ────────────────────────────────────────────────────────
    if (ui.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAddSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            AddSaveSheetContent(
                isScanning = ui.isScanning,
                detected = ui.detectedFiles,
                onSelect = viewModel::addDetected,
                onPickManually = { filePicker.launch(arrayOf("*/*")); viewModel.closeAddSheet() },
                onDismiss = viewModel::closeAddSheet,
            )
        }
    }

    if (showPermissionDialog) {
        RequestAllFilesAccessDialog(onDismiss = { showPermissionDialog = false })
    }
}

// ── Saves drawer ───────────────────────────────────────────────────────────────

@Composable
private fun SavesDrawer(
    saves: List<RegisteredSave>,
    activeSave: RegisteredSave?,
    onSelectSave: (RegisteredSave) -> Unit,
    onRemoveSave: (String) -> Unit,
    onAddSave: () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxSize()) {
            Text(
                "PokeSync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 4.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Save files",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            HorizontalDivider()
            if (saves.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No saves yet.\nTap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(saves, key = { it.id }) { save ->
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(save.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        save.emulator + (save.generation?.let { " • Gen $it" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            selected = save.id == activeSave?.id,
                            onClick = { onSelectSave(save) },
                            badge = {
                                IconButton(onClick = { onRemoveSave(save.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
            HorizontalDivider()
            OutlinedButton(onClick = onAddSave, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Save File")
            }
        }
    }
}

// ── Save panel ─────────────────────────────────────────────────────────────────

@Composable
private fun SavePanel(
    pokemon: List<Pokemon>,
    currentBox: Int,
    boxCount: Int,
    selectedPokemon: Pokemon?,
    isLoading: Boolean,
    hasActiveSave: Boolean,
    isDragging: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectPokemon: (Pokemon) -> Unit,
    onAddSave: () -> Unit,
    onDragStart: (Pokemon, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    when {
        !hasActiveSave -> EmptyPanel("No save file loaded.\nTap + to add your first save.", "Add Save", onAddSave)
        isLoading -> LoadingPanel()
        else -> BoxGrid(
            pokemon = pokemon,
            currentBox = currentBox,
            boxCount = boxCount,
            selectedPokemon = selectedPokemon,
            isDraggable = true,
            isDragging = isDragging,
            slotBoundsMap = null,
            onPrev = onPrev,
            onNext = onNext,
            onSelectPokemon = onSelectPokemon,
            onDragStart = onDragStart,
            onDragMove = onDragMove,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
    }
}

// ── Bank panel ─────────────────────────────────────────────────────────────────

@Composable
private fun BankPanel(
    pokemon: List<Pokemon>,
    currentBox: Int,
    boxCount: Int,
    selectedPokemon: Pokemon?,
    isLoading: Boolean,
    hasSaveLoaded: Boolean,
    isDragging: Boolean,
    dragPosition: Offset,
    slotBoundsMap: MutableMap<Int, Rect>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectPokemon: (Pokemon) -> Unit,
    onImportSave: () -> Unit,
    onDragStart: (Pokemon, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (hasSaveLoaded && pokemon.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Import your save to store Pokémon permanently in the Vault.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onImportSave, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Import", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        when {
            isLoading && pokemon.isEmpty() -> LoadingPanel()
            pokemon.isNotEmpty() || isLoading -> BoxGrid(
                pokemon = pokemon,
                currentBox = currentBox,
                boxCount = boxCount,
                selectedPokemon = selectedPokemon,
                isDraggable = true,
                isDragging = isDragging,
                slotBoundsMap = slotBoundsMap,
                onPrev = onPrev,
                onNext = onNext,
                onSelectPokemon = onSelectPokemon,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
            )
            !hasSaveLoaded -> EmptyPanel("Your Vault is empty.\nLoad a save and import Pokémon.", null, null)
        }
    }
}

// ── Box grid ───────────────────────────────────────────────────────────────────

@Composable
private fun BoxGrid(
    pokemon: List<Pokemon>,
    currentBox: Int,
    boxCount: Int,
    selectedPokemon: Pokemon?,
    isDraggable: Boolean,
    isDragging: Boolean,
    slotBoundsMap: MutableMap<Int, Rect>?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectPokemon: (Pokemon) -> Unit,
    onDragStart: (Pokemon, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val slots = remember(pokemon, currentBox) {
        val arr = arrayOfNulls<Pokemon>(30)
        pokemon.filter { it.box == currentBox }.forEach { p ->
            arr[p.slot.coerceIn(0, 29)] = p
        }
        arr
    }

    Column(Modifier.fillMaxSize()) {
        // Box navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev, enabled = currentBox > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous box")
            }
            Text(
                "Box ${currentBox + 1}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            IconButton(onClick = onNext, enabled = currentBox < boxCount - 1) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next box")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(30) { slotIdx ->
                BoxSlot(
                    pokemon = slots[slotIdx],
                    isSelected = slots[slotIdx] != null && slots[slotIdx] == selectedPokemon,
                    isDraggable = isDraggable && !isDragging,
                    onGloballyPositioned = slotBoundsMap?.let { map ->
                        { bounds -> map[slotIdx] = bounds }
                    },
                    onClick = { slots[slotIdx]?.let(onSelectPokemon) },
                    onDragStart = { pkm, pos -> onDragStart(pkm, pos) },
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            }
        }
    }
}

@Composable
private fun BoxSlot(
    pokemon: Pokemon?,
    isSelected: Boolean,
    isDraggable: Boolean,
    onGloballyPositioned: ((Rect) -> Unit)?,
    onClick: () -> Unit,
    onDragStart: (Pokemon, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var boundsInRoot by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (pokemon != null) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (isSelected) Modifier.border(2.dp, HomeYellow, RoundedCornerShape(6.dp))
                else Modifier
            )
            .onGloballyPositioned { coords ->
                boundsInRoot = coords.boundsInRoot()
                onGloballyPositioned?.invoke(boundsInRoot)
            }
            .then(
                if (isDraggable && pokemon != null) {
                    Modifier.pointerInput(pokemon) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val globalStart = Offset(
                                    boundsInRoot.left + offset.x,
                                    boundsInRoot.top + offset.y,
                                )
                                onDragStart(pokemon, globalStart)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragMove(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragCancel() },
                        )
                    }
                } else Modifier
            )
            .clickable(enabled = pokemon != null && !isDraggable.not(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (pokemon != null) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.species,
                modifier = Modifier.fillMaxSize(0.9f),
            )
            if (pokemon.isShiny) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Shiny",
                    tint = HomeYellow,
                    modifier = Modifier.align(Alignment.TopEnd).size(10.dp),
                )
            }
        } else {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape))
        }
    }
}

// ── Pokémon detail sheet ───────────────────────────────────────────────────────

@Composable
private fun PokemonDetailSheet(
    pokemon: Pokemon,
    isBankPokemon: Boolean,
    onStoreToBankClick: () -> Unit,
    onRemoveFromBankClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(model = pokemon.spriteUrl, contentDescription = pokemon.species, modifier = Modifier.size(88.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pokemon.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (pokemon.isShiny) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Star, "Shiny", tint = HomeYellow, modifier = Modifier.size(16.dp))
                    }
                }
                if (pokemon.nickname != null) {
                    Text(pokemon.species, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    buildString {
                        append("Lv.${pokemon.level}")
                        pokemon.genderSymbol?.let { append("  $it") }
                        pokemon.nature?.let { append("  $it") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Gen ${pokemon.generation}" + (pokemon.ball?.let { "  •  $it Ball" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (!isBankPokemon) {
            Button(
                onClick = { onStoreToBankClick(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Store in Vault")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Or long-press and drag to the Vault tab",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            OutlinedButton(
                onClick = { onRemoveFromBankClick(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Remove from Vault")
            }
        }
    }
}

// ── Add save sheet ─────────────────────────────────────────────────────────────

@Composable
private fun AddSaveSheetContent(
    isScanning: Boolean,
    detected: List<DetectedSaveFile>,
    onSelect: (DetectedSaveFile) -> Unit,
    onPickManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Add Save File", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        when {
            isScanning -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Scanning emulators…")
                }
            }
            detected.isEmpty() -> Text(
                "No Pokémon saves found in known emulator directories.\nPick a file manually instead.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> detected.groupBy { it.emulator }.forEach { (emulator, files) ->
                Text(emulator, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                files.forEach { file ->
                    ListItem(
                        headlineContent = { Text(file.gameName ?: file.file.nameWithoutExtension) },
                        supportingContent = {
                            val sub = file.gameName?.let { file.file.nameWithoutExtension } ?: file.file.parentFile?.name
                            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall)
                        },
                        trailingContent = {
                            IconButton(onClick = { onSelect(file); onDismiss() }) {
                                Icon(Icons.Default.Add, "Add")
                            }
                        },
                    )
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onPickManually, modifier = Modifier.fillMaxWidth()) {
            Text("Pick File Manually")
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Generic states ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyPanel(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
