package com.pokesync.android.ui.screens.home

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pokesync.android.data.local.DetectedSaveFile
import com.pokesync.android.domain.model.Pokemon
import com.pokesync.android.domain.model.PokemonMove
import com.pokesync.android.domain.model.RegisteredSave
import com.pokesync.android.ui.permission.RequestAllFilesAccessDialog
import com.pokesync.android.ui.permission.hasAllFilesAccess
import com.pokesync.android.ui.theme.HomeYellow
import kotlin.math.cos
import kotlin.math.sin

// ── Type color palette ─────────────────────────────────────────────────────────

private val typeColorMap = mapOf(
    "Normal"   to Color(0xFFA8A878),
    "Fire"     to Color(0xFFF08030),
    "Water"    to Color(0xFF6890F0),
    "Electric" to Color(0xFFF8D030),
    "Grass"    to Color(0xFF78C850),
    "Ice"      to Color(0xFF98D8D8),
    "Fighting" to Color(0xFFC03028),
    "Poison"   to Color(0xFFA040A0),
    "Ground"   to Color(0xFFE0C068),
    "Flying"   to Color(0xFFA890F0),
    "Psychic"  to Color(0xFFF85888),
    "Bug"      to Color(0xFFA8B820),
    "Rock"     to Color(0xFFB8A038),
    "Ghost"    to Color(0xFF705898),
    "Dragon"   to Color(0xFF7038F8),
    "Dark"     to Color(0xFF705848),
    "Steel"    to Color(0xFFB8B8D0),
    "Fairy"    to Color(0xFFEE99AC),
)

private fun typeColor(type: String) = typeColorMap[type] ?: Color(0xFFA8A878)

// ── Colours ────────────────────────────────────────────────────────────────────

private val BgGradient   = Brush.linearGradient(listOf(Color(0xFF3EBDAD), Color(0xFF72E8D8)))
private val PanelBg      = Color(0xFF4DCFBE).copy(alpha = 0.75f)
private val SlotEmpty    = Color(0xFF3A9AAA).copy(alpha = 0.6f)
private val SlotFilled   = Color(0xFF5BC4B8).copy(alpha = 0.5f)
private val HeaderPill   = Color(0xFF1F7A6E)
private val RedLabel     = Color(0xFFD32F2F)
private val SummaryBg    = Color(0xFFCCF0F9)
private val SelectBorder = Color(0xFF1A4FA0)
private val SidebarRed   = Brush.verticalGradient(listOf(Color(0xFFCC1111), Color(0xFF880000)))

private const val SLOTS_PER_BOX = 30

// ── Drag state ─────────────────────────────────────────────────────────────────

private data class DragState(
    val pokemon: Pokemon,
    val source: HomeTab,
    val position: Offset,  // window-space coordinates of ghost centre
)

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToConnect: () -> Unit = {},
) {
    val saves by viewModel.saves.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }
    var showSaveDropdown by remember { mutableStateOf(false) }
    var isBoxView by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(!hasAllFilesAccess()) }

    // Drag and drop state
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var leftPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var rightPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var rightGridBounds by remember { mutableStateOf<Rect?>(null) }

    fun handleDrop(ds: DragState) {
        val inRight = rightPanelBounds?.contains(ds.position) == true
        val inLeft = leftPanelBounds?.contains(ds.position) == true
        when (ds.source) {
            HomeTab.Save -> {
                if (inRight) viewModel.addPokemonToVault(ds.pokemon)
            }
            HomeTab.Bank -> when {
                inRight -> {
                    val gb = rightGridBounds ?: return
                    if (!gb.contains(ds.position)) return
                    val cols = 4
                    val rows = (SLOTS_PER_BOX + cols - 1) / cols
                    val col = ((ds.position.x - gb.left) / (gb.width / cols)).toInt().coerceIn(0, cols - 1)
                    val row = ((ds.position.y - gb.top) / (gb.height / rows)).toInt().coerceIn(0, rows - 1)
                    val slot = (row * cols + col).coerceIn(0, SLOTS_PER_BOX - 1)
                    viewModel.onVaultSlotDrop(ds.pokemon, ui.bankBox, slot)
                }
                inLeft -> viewModel.exportToSave(ds.pokemon.id)
            }
        }
    }

    val onDragStart: (Pokemon, HomeTab, Offset) -> Unit = { pkm, src, pos ->
        dragState = DragState(pkm, src, pos)
    }
    val onDragUpdate: (Offset) -> Unit = { delta ->
        dragState = dragState?.copy(position = dragState!!.position + delta)
    }
    val onDragEnd: () -> Unit = {
        dragState?.let { handleDrop(it) }
        dragState = null
    }

    // Force landscape
    val activity = LocalContext.current as? Activity
    SideEffect { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addFromUri(it) }
    }
    LaunchedEffect(ui.globalError) {
        ui.globalError?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val selectedFromSave = ui.selectedPokemon != null && ui.selectedPokemonSource == HomeTab.Save
    val selectedFromBank = ui.selectedPokemon != null && ui.selectedPokemonSource == HomeTab.Bank

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        // ── Two-panel layout ───────────────────────────────────────────────────
        Row(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // LEFT panel — track bounds for drop detection (export from vault → save)
            HomePanel(
                Modifier
                    .weight(1f)
                    .onGloballyPositioned { leftPanelBounds = it.boundsInWindow() }
            ) {
                if (selectedFromBank) {
                    SummaryContent(
                        pokemon = ui.selectedPokemon!!,
                        onClose = { viewModel.selectPokemon(null, null) },
                        onAddToVault = null,
                        onExportToSave = { viewModel.exportToSave(ui.selectedPokemon!!.id) },
                        onDelete = { viewModel.requestDeletePokemon(ui.selectedPokemon!!) },
                    )
                } else {
                    SaveContent(
                        saves = saves,
                        ui = ui,
                        isBoxView = isBoxView,
                        showDropdown = showSaveDropdown,
                        onOpenSettings = { showSettings = true },
                        onToggleDropdown = { showSaveDropdown = !showSaveDropdown },
                        onSelectSave = { viewModel.selectSave(it); showSaveDropdown = false },
                        onRemoveSave = { viewModel.removeSave(it); showSaveDropdown = false },
                        onPrev = viewModel::prevSaveBox,
                        onNext = viewModel::nextSaveBox,
                        onSelectBox = { },
                        onSelectPokemon = { pkm ->
                            if (pkm.id == ui.selectedPokemon?.id) viewModel.selectPokemon(null, null)
                            else viewModel.selectPokemon(pkm, HomeTab.Save)
                        },
                        selectedPokemon = ui.selectedPokemon,
                        activeDrag = dragState,
                        onDragStart = { pkm, pos -> onDragStart(pkm, HomeTab.Save, pos) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd = onDragEnd,
                    )
                }
            }
            // RIGHT panel — track bounds for drop detection
            HomePanel(
                Modifier
                    .weight(1f)
                    .onGloballyPositioned { rightPanelBounds = it.boundsInWindow() }
            ) {
                if (selectedFromSave) {
                    SummaryContent(
                        pokemon = ui.selectedPokemon!!,
                        onClose = { viewModel.selectPokemon(null, null) },
                        onAddToVault = { viewModel.addPokemonToVault(ui.selectedPokemon!!) },
                    )
                } else {
                    BankContent(
                        ui = ui,
                        isBoxView = isBoxView,
                        onToggleBoxView = { isBoxView = !isBoxView },
                        onPrev = viewModel::prevBankBox,
                        onNext = viewModel::nextBankBox,
                        onSelectBox = { },
                        onSelectPokemon = { pkm ->
                            if (pkm.id == ui.selectedPokemon?.id) viewModel.selectPokemon(null, null)
                            else viewModel.selectPokemon(pkm, HomeTab.Bank)
                        },
                        onImportSave = { viewModel.importSaveToBank(replace = false) },
                        selectedPokemon = ui.selectedPokemon,
                        activeDrag = dragState,
                        onDragStart = { pkm, pos -> onDragStart(pkm, HomeTab.Bank, pos) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd = onDragEnd,
                        onGridPositioned = { rightGridBounds = it },
                    )
                }
            }
        }

        // ── Drag ghost ─────────────────────────────────────────────────────────
        dragState?.let { ds ->
            val ghostDp = 72.dp
            val density = LocalDensity.current
            val ghostPx = with(density) { ghostDp.toPx() }
            val offsetX = with(density) { (ds.position.x - ghostPx / 2).toDp() }
            val offsetY = with(density) { (ds.position.y - ghostPx / 2).toDp() }
            Box(
                Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(ghostDp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlotFilled)
                    .border(2.dp, SelectBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ds.pokemon.spriteUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.88f),
                )
            }
        }

        // ── Settings sidebar overlay ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            SettingsSidebar(
                onClose = { showSettings = false },
                onManageSaves = { showSettings = false; viewModel.openAddSheet() },
                onRefreshSaves = { viewModel.syncActiveSave(); showSettings = false },
                onServerSettings = { showSettings = false; onNavigateToConnect() },
            )
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ── Add save sheet ─────────────────────────────────────────────────────────
    if (ui.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAddSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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

    if (ui.pokemonToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete Pokémon") },
            text = {
                Text(
                    "This will permanently remove ${ui.pokemonToDelete!!.displayName} from your vault. " +
                    "This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = RedLabel),
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::cancelDelete) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showPermissionDialog) {
        RequestAllFilesAccessDialog(onDismiss = { showPermissionDialog = false })
    }
}

// ── Panel container ────────────────────────────────────────────────────────────

@Composable
private fun HomePanel(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelBg)
            .padding(8.dp),
    ) { content() }
}

// ── Save panel ─────────────────────────────────────────────────────────────────

@Composable
private fun SaveContent(
    saves: List<RegisteredSave>,
    ui: HomeUiState,
    isBoxView: Boolean,
    showDropdown: Boolean,
    selectedPokemon: Pokemon?,
    onOpenSettings: () -> Unit,
    onToggleDropdown: () -> Unit,
    onSelectSave: (RegisteredSave) -> Unit,
    onRemoveSave: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectBox: (Int) -> Unit,
    onSelectPokemon: (Pokemon) -> Unit,
    activeDrag: DragState? = null,
    onDragStart: ((Pokemon, Offset) -> Unit)? = null,
    onDragUpdate: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Menu, null, tint = Color.White)
            }
            IconButton(onClick = onPrev, enabled = ui.saveBox > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Box(Modifier.weight(1f)) {
                Button(
                    onClick = onToggleDropdown,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderPill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        ui.activeSave?.displayName ?: "No save",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showDropdown, onDismissRequest = onToggleDropdown) {
                    saves.forEach { save ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(save.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    save.emulator.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
                                }
                            },
                            onClick = { onSelectSave(save) },
                            trailingIcon = {
                                IconButton(onClick = { onRemoveSave(save.id) }) {
                                    Icon(Icons.Default.Close, "Remove save", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            },
                        )
                    }
                    if (saves.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No saves registered", color = Color.Gray) },
                            onClick = {},
                        )
                    }
                }
            }
            IconButton(onClick = onNext, enabled = ui.saveBox < ui.saveBoxCount - 1) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }

        // Box label
        Text(
            "Box ${ui.saveBox + 1}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp),
        )

        when {
            ui.isSaveLoading -> LoadingPanel()
            ui.activeSave == null -> EmptyPanel("No save loaded.\nTap ≡ → Manage saves to add one.")
            isBoxView -> BoxThumbnailGrid(
                allPokemon = ui.savePokemon,
                currentBox = ui.saveBox,
                boxCount = ui.saveBoxCount,
                onSelectBox = onSelectBox,
            )
            else -> BoxGrid(
                pokemon = ui.savePokemon,
                currentBox = ui.saveBox,
                selectedPokemon = selectedPokemon,
                onSelectPokemon = onSelectPokemon,
                activeDrag = activeDrag,
                onDragStart = onDragStart,
                onDragUpdate = onDragUpdate,
                onDragEnd = onDragEnd,
            )
        }
    }
}

// ── Bank panel ─────────────────────────────────────────────────────────────────

@Composable
private fun BankContent(
    ui: HomeUiState,
    isBoxView: Boolean,
    selectedPokemon: Pokemon?,
    onToggleBoxView: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectBox: (Int) -> Unit,
    onSelectPokemon: (Pokemon) -> Unit,
    onImportSave: () -> Unit,
    activeDrag: DragState? = null,
    onDragStart: ((Pokemon, Offset) -> Unit)? = null,
    onDragUpdate: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onGridPositioned: ((Rect) -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev, enabled = ui.bankBox > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HeaderPill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Vault – Box ${ui.bankBox + 1}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onNext, enabled = ui.bankBox < ui.bankBoxCount - 1) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
            IconButton(onClick = onToggleBoxView) {
                Icon(
                    if (isBoxView) Icons.Default.ViewModule else Icons.Default.GridView,
                    null,
                    tint = Color.White,
                )
            }
        }

        when {
            ui.isBankLoading -> LoadingPanel()
            isBoxView -> BoxThumbnailGrid(
                allPokemon = ui.bankPokemon,
                currentBox = ui.bankBox,
                boxCount = ui.bankBoxCount,
                onSelectBox = onSelectBox,
            )
            ui.bankPokemon.isEmpty() && ui.activeSave != null -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Vault is empty.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onImportSave,
                        colors = ButtonDefaults.buttonColors(containerColor = HeaderPill),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Import from save", color = Color.White)
                    }
                }
            }
            else -> BoxGrid(
                pokemon = ui.bankPokemon,
                currentBox = ui.bankBox,
                selectedPokemon = selectedPokemon,
                onSelectPokemon = onSelectPokemon,
                activeDrag = activeDrag,
                onDragStart = onDragStart,
                onDragUpdate = onDragUpdate,
                onDragEnd = onDragEnd,
                onGridPositioned = onGridPositioned,
            )
        }
    }
}

// ── Box grid ───────────────────────────────────────────────────────────────────

@Composable
private fun BoxGrid(
    pokemon: List<Pokemon>,
    currentBox: Int,
    selectedPokemon: Pokemon?,
    onSelectPokemon: (Pokemon) -> Unit,
    activeDrag: DragState? = null,
    onDragStart: ((Pokemon, Offset) -> Unit)? = null,
    onDragUpdate: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onGridPositioned: ((Rect) -> Unit)? = null,
) {
    val slots = remember(pokemon, currentBox) {
        val arr = arrayOfNulls<Pokemon>(SLOTS_PER_BOX)
        pokemon.filter { it.box == currentBox }.forEach { p ->
            if (p.slot in 0 until SLOTS_PER_BOX) arr[p.slot] = p
        }
        arr
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (onGridPositioned != null)
                    Modifier.onGloballyPositioned { onGridPositioned(it.boundsInWindow()) }
                else Modifier
            ),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = activeDrag == null,
    ) {
        items(SLOTS_PER_BOX) { idx ->
            val pkm = slots[idx]
            val isBeingDragged = activeDrag?.pokemon?.id == pkm?.id
            var slotWindowPos by remember { mutableStateOf(Offset.Zero) }
            var slotSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (pkm != null) SlotFilled else SlotEmpty)
                    .then(
                        if (pkm != null && pkm == selectedPokemon)
                            Modifier.border(2.dp, SelectBorder, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .alpha(if (isBeingDragged) 0.3f else 1f)
                    .onGloballyPositioned { coords ->
                        slotWindowPos = coords.positionInWindow()
                        slotSize = coords.size
                    }
                    .then(
                        if (pkm != null && onDragStart != null) {
                            Modifier.pointerInput(pkm.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        val centre = slotWindowPos + Offset(
                                            slotSize.width / 2f,
                                            slotSize.height / 2f,
                                        )
                                        onDragStart(pkm, centre)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        onDragUpdate?.invoke(Offset(amount.x, amount.y))
                                    },
                                    onDragEnd = { onDragEnd?.invoke() },
                                    onDragCancel = { onDragEnd?.invoke() },
                                )
                            }
                        } else Modifier
                    )
                    .clickable(enabled = pkm != null && activeDrag == null) {
                        pkm?.let(onSelectPokemon)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (pkm != null) {
                    AsyncImage(
                        model = pkm.spriteUrl,
                        contentDescription = pkm.species,
                        modifier = Modifier.fillMaxSize(0.88f),
                    )
                    if (pkm.isShiny) {
                        Icon(
                            Icons.Default.Star,
                            "Shiny",
                            tint = HomeYellow,
                            modifier = Modifier.size(10.dp).align(Alignment.TopEnd),
                        )
                    }
                }
            }
        }
    }
}

// ── Box thumbnail grid ─────────────────────────────────────────────────────────

@Composable
private fun BoxThumbnailGrid(
    allPokemon: List<Pokemon>,
    currentBox: Int,
    boxCount: Int,
    onSelectBox: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(boxCount) { boxIdx ->
            val occupied = remember(allPokemon, boxIdx) {
                allPokemon.filter { it.box == boxIdx }.map { it.slot }.toSet()
            }
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (boxIdx == currentBox) HeaderPill else SlotEmpty)
                    .clickable { onSelectBox(boxIdx) }
                    .padding(6.dp),
            ) {
                // 5 cols × 4 rows = 20 dot mini-grid
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(4) { row ->
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(5) { col ->
                                val slot = row * 5 + col
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (slot in occupied) Color(0xFF1A4FA0)
                                            else Color.White.copy(alpha = 0.6f)
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Summary panel ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryContent(
    pokemon: Pokemon,
    onClose: () -> Unit,
    onAddToVault: (() -> Unit)?,
    onExportToSave: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        // Header row
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HeaderPill)
                    .clickable { onClose() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Summary  ×", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            if (onAddToVault != null) {
                Button(
                    onClick = onAddToVault,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedLabel),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("+ Vault", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (onExportToSave != null) {
                Button(
                    onClick = onExportToSave,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderPill),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Export", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (onDelete != null) {
                Button(
                    onClick = onDelete,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedLabel),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("×", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(SummaryBg)
                .padding(10.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                // Sprite + info rows
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = pokemon.spriteUrl,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        InfoRow("Nickname", pokemon.displayName, italic = pokemon.nickname != null)
                        InfoRow("Species", pokemon.species)
                        InfoRow("Level", pokemon.level.toString())
                        pokemon.nature?.let { InfoRow("Nature", it) }
                        if (pokemon.type1.isNotBlank()) InfoRow("Type", pokemon.typeString)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Stats + Moves
                Row(Modifier.weight(1f)) {
                    Column(Modifier.weight(1f)) {
                        SectionHeader("Stats")
                        if (pokemon.hasStats) {
                            StatsRadarChart(
                                hp = pokemon.statHp, atk = pokemon.statAtk, def = pokemon.statDef,
                                spa = pokemon.statSpa, spd = pokemon.statSpd, spe = pokemon.statSpe,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("—", color = Color.Gray)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        SectionHeader("Moves")
                        Spacer(Modifier.height(4.dp))
                        pokemon.moves.forEach { move ->
                            MoveButton(move)
                            Spacer(Modifier.height(4.dp))
                        }
                        if (pokemon.moves.isEmpty()) {
                            Text("No move data", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, italic: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(RedLabel)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                value,
                style = if (italic) MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                        else MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(RedLabel)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MoveButton(move: PokemonMove) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(typeColor(move.type))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            move.name,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Radar chart ────────────────────────────────────────────────────────────────

@Composable
private fun StatsRadarChart(
    hp: Int, atk: Int, def: Int, spa: Int, spd: Int, spe: Int,
    modifier: Modifier,
) {
    // Order around the hexagon (starting top, clockwise): HP, Atk, Def, Spe, SpD, SpA
    val values = listOf(hp, atk, def, spe, spd, spa).map { it.toFloat() }
    val labels = listOf("HP", "Atk", "Def", "Spe", "SpD", "SpA")
    val nums   = listOf(hp, atk, def, spe, spd, spa)
    val maxVal = 400f

    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r  = minOf(size.width, size.height) / 2 * 0.58f

        val angles = (0 until 6).map { i -> (-Math.PI / 2 + Math.PI * 2 / 6 * i).toFloat() }

        // Background rings
        for (frac in listOf(0.25f, 0.5f, 0.75f, 1f)) {
            val path = Path()
            angles.forEachIndexed { i, a ->
                val x = cx + r * frac * cos(a); val y = cy + r * frac * sin(a)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color(0x22FFFFFF))
            drawPath(path, Color(0x55888888), style = Stroke(0.5.dp.toPx()))
        }
        // Spokes
        angles.forEach { a ->
            drawLine(Color(0x44888888), center, androidx.compose.ui.geometry.Offset(cx + r * cos(a), cy + r * sin(a)), 0.5.dp.toPx())
        }
        // Stat polygon
        val poly = Path()
        values.forEachIndexed { i, v ->
            val ratio = (v / maxVal).coerceIn(0f, 1f)
            val x = cx + r * ratio * cos(angles[i]); val y = cy + r * ratio * sin(angles[i])
            if (i == 0) poly.moveTo(x, y) else poly.lineTo(x, y)
        }
        poly.close()
        drawPath(poly, Color(0x88FFB7C5))
        drawPath(poly, Color(0xFFFF6B9D), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ── Settings sidebar ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSidebar(
    onClose: () -> Unit,
    onManageSaves: () -> Unit,
    onRefreshSaves: () -> Unit,
    onServerSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(SidebarRed)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Settings",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            SidebarButton("Manage saves", onManageSaves)
            Spacer(Modifier.height(12.dp))
            SidebarButton("Server Settings", onServerSettings)
            Spacer(Modifier.height(12.dp))
            SidebarButton("Refresh saves", onRefreshSaves)
        }
    }
}

@Composable
private fun SidebarButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3EBDAD)),
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
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
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
            else -> LazyColumn {
                detected.groupBy { it.emulator }.forEach { (emulator, files) ->
                    item {
                        Text(
                            emulator,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(files) { file ->
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
                    item { HorizontalDivider() }
                }
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
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    }
}
