package com.example.rotarylauncher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.rotarylauncher.ui.theme.LabelStyle
import java.util.UUID

private const val FAVORITES_TAB_ID = "FAVORITES_TAB"
private const val MAX_FOLDERS = 20
private const val HAIRLINE_ALPHA = 0.2f

@Composable
fun AppManagerScreen(allApps: List<AppEntry>) {
    var selectedTabId by remember { mutableStateOf(FAVORITES_TAB_ID) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabChip("Favorites", selectedTabId == FAVORITES_TAB_ID, onBg, muted) { selectedTabId = FAVORITES_TAB_ID }
                    FoldersConfig.folders.value.forEach { folder ->
                        TabChip(folder.name.toTitleCaseSmart(), selectedTabId == folder.id, onBg, muted) { selectedTabId = folder.id }
                    }
                }
                IconButton(onClick = {
                    if (FoldersConfig.folders.value.size < MAX_FOLDERS) {
                        val newFolder = Folder(id = UUID.randomUUID().toString(), name = "New Folder")
                        FoldersConfig.folders.value = FoldersConfig.folders.value + newFolder
                        selectedTabId = newFolder.id
                    }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add folder", tint = onBg) }
            }

            Spacer(Modifier.height(8.dp))

            if (selectedTabId == FAVORITES_TAB_ID) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FavoritesConfig.slots().forEachIndexed { index, slot ->
                        val app = allApps.find { it.packageName == slot.value }
                        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            if (app != null) IconTile(app = app, size = 48.dp) { slot.value = null }
                            else Text("${index + 1}", color = muted, style = LabelStyle)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Tap Bottom App To Fill Next Empty Slot", color = muted, style = LabelStyle)
            } else {
                val folder = FoldersConfig.folders.value.find { it.id == selectedTabId }
                if (folder != null) {
                    val folderIndex = FoldersConfig.folders.value.indexOf(folder)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(folder.name.toTitleCaseSmart(), color = onBg, style = LabelStyle, modifier = Modifier.weight(1f))
                        Text("◀", color = muted, modifier = Modifier.clickable {
                            if (folderIndex > 0) {
                                val list = FoldersConfig.folders.value.toMutableList()
                                list.add(folderIndex - 1, list.removeAt(folderIndex))
                                FoldersConfig.folders.value = list
                            }
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("▶", color = muted, modifier = Modifier.clickable {
                            if (folderIndex < FoldersConfig.folders.value.lastIndex) {
                                val list = FoldersConfig.folders.value.toMutableList()
                                list.add(folderIndex + 1, list.removeAt(folderIndex))
                                FoldersConfig.folders.value = list
                            }
                        })
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { renamingId = folder.id; renameText = folder.name }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = muted)
                        }
                        IconButton(onClick = { pendingDeleteId = folder.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFE5533D).copy(alpha = 0.7f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Long-Press An App To Drag And Reorder", color = muted, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    ReorderableFolderGrid(folder = folder, allApps = allApps)
                }
            }
        }

        val assignedPackages = FoldersConfig.folders.value.flatMap { it.appPackageNames }.toSet()
        val poolApps = if (selectedTabId == FAVORITES_TAB_ID) allApps else allApps.filter { it.packageName !in assignedPackages }

        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(2f).fillMaxWidth().padding(12.dp)) {
            items(poolApps, key = { it.packageName }) { app ->
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconTile(app = app, size = 48.dp) {
                        if (selectedTabId == FAVORITES_TAB_ID) {
                            FavoritesConfig.assignFirstEmpty(app.packageName)
                        } else {
                            val folder = FoldersConfig.folders.value.find { it.id == selectedTabId }
                            if (folder != null && app.packageName !in folder.appPackageNames) folder.appPackageNames.add(app.packageName)
                        }
                    }
                    Text(app.label.toTitleCaseSmart(), color = onBg, fontSize = 9.sp, maxLines = 1, style = LabelStyle)
                }
            }
        }
    }

    if (renamingId != null) {
        Dialog(onDismissRequest = { renamingId = null }) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)).padding(16.dp).imePadding()) {
                Text("Rename Folder", color = onBg, style = LabelStyle)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = { renamingId = null }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val folder = FoldersConfig.folders.value.find { it.id == renamingId }
                        folder?.name = renameText.ifBlank { folder?.name ?: "New Folder" }
                        FoldersConfig.folders.value = FoldersConfig.folders.value.toList()
                        renamingId = null
                    }) { Text("Save") }
                }
            }
        }
    }

    if (pendingDeleteId != null) {
        val folder = FoldersConfig.folders.value.find { it.id == pendingDeleteId }
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete '${folder?.name}'?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        FoldersConfig.folders.value = FoldersConfig.folders.value.filter { it.id != pendingDeleteId }
                        if (selectedTabId == pendingDeleteId) selectedTabId = FAVORITES_TAB_ID
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5533D))
                ) { Text("DELETE") }
            },
            dismissButton = { Button(onClick = { pendingDeleteId = null }) { Text("CANCEL") } }
        )
    }
}

/**
 * First-pass live-reorder grid. All items compose simultaneously (not lazy) so their
 * bounds can be tracked directly — fine for realistic folder sizes (tens of apps),
 * would need rework for hundreds. Long-press starts a drag; nearest-cell-center wins
 * as the live swap target; release commits whatever order resulted.
 */
@Composable
private fun ReorderableFolderGrid(folder: Folder, allApps: List<AppEntry>, columns: Int = 5) {
    val folderApps = folder.appPackageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }

    Column {
        var globalIndex = 0
        folderApps.chunked(columns).forEach { rowApps ->
            Row {
                rowApps.forEach { app ->
                    val index = globalIndex
                    globalIndex++
                    val isDragging = draggingIndex == index

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .onGloballyPositioned { coords -> itemBounds[index] = coords.boundsInParent() }
                            .graphicsLayer {
                                if (isDragging) { translationX = dragOffset.x; translationY = dragOffset.y }
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .pointerInputSafeDrag(
                                onDragStart = { draggingIndex = index; dragOffset = Offset.Zero },
                                onDrag = { amount ->
                                    dragOffset += amount
                                    val currentIdx = draggingIndex ?: return@pointerInputSafeDrag
                                    val currentBounds = itemBounds[currentIdx] ?: return@pointerInputSafeDrag
                                    val draggedCenter = currentBounds.center + dragOffset
                                    val targetIndex = itemBounds.entries
                                        .filter { it.key != currentIdx }
                                        .minByOrNull { (it.value.center - draggedCenter).getDistance() }
                                        ?.key
                                    if (targetIndex != null) {
                                        val newBounds = itemBounds[targetIndex]
                                        val oldBounds = currentBounds
                                        val pkg = folder.appPackageNames.removeAt(currentIdx)
                                        folder.appPackageNames.add(targetIndex, pkg)
                                        draggingIndex = targetIndex
                                        if (newBounds != null) dragOffset += (oldBounds.topLeft - newBounds.topLeft)
                                    }
                                },
                                onDragEnd = { draggingIndex = null; dragOffset = Offset.Zero }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconTile(app = app, size = 44.dp) { folder.appPackageNames.remove(app.packageName) }
                    }
                }
            }
        }
    }
}

private fun Modifier.pointerInputSafeDrag(
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart() },
            onDrag = { change, amount -> change.consume(); onDrag(amount) },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() }
        )
    }
)

// Thin wrapper kept separate purely so the long, fully-qualified pointerInput call
// above stays readable — functionally identical to calling pointerInput directly.
/*
@Suppress("FunctionName")
private fun Modifier.androidx_compose_ui_input_pointer_pointerInput_workaround(
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier = androidx.compose.ui.input.pointer.pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDragStart = { onDragStart() },
        onDrag = { change, amount -> change.consume(); onDrag(amount) },
        onDragEnd = { onDragEnd() },
        onDragCancel = { onDragEnd() }
    )
}
*/

@Composable
private fun IconTile(app: AppEntry, size: Dp, onClick: () -> Unit) {
    val density = LocalDensity.current
    val bg = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = HAIRLINE_ALPHA)
    val gradient = remember(size) {
        Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = 0.03f), Color.Transparent), radius = with(density) { size.toPx() } * 0.9f)
    }
    Box(
        modifier = Modifier.size(size).background(bg, RoundedCornerShape(14.dp)).background(gradient, RoundedCornerShape(14.dp))
            .border(1.dp, outline, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(size * 0.8f))
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onBg: Color, muted: Color, onClick: () -> Unit) {
    Text(label, color = if (selected) onBg else muted, style = LabelStyle, modifier = Modifier.padding(vertical = 6.dp).clickable(onClick = onClick))
}