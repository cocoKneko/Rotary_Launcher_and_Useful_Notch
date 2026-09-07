package com.example.rotarylauncher

import android.content.Context
import androidx.compose.runtime.MutableState

data class WheelUiState(val rowIndex: Int = 0, val columnIndex: Int = 0)

class HomeInputHandler(
    private val categories: List<Category>,
    private val state: MutableState<WheelUiState>,
    private val context: Context,
) : InputHandler {

    override fun onRotate(detents: Int) = moveColumn(detents)

    override fun onDpad(direction: DpadDirection) {
        val invert = WheelConfig.dpadVerticalInverted.value
        when (direction) {
            DpadDirection.UP -> moveColumn(if (invert) 1 else -1)
            DpadDirection.DOWN -> moveColumn(if (invert) -1 else 1)
            DpadDirection.LEFT -> moveRow(-1)
            DpadDirection.RIGHT -> moveRow(1)
        }
    }

    private fun moveColumn(delta: Int) {
        val row = categories.getOrNull(state.value.rowIndex) ?: return
        if (row.apps.isEmpty()) return
        val newCol = (state.value.columnIndex + delta).mod(row.apps.size)
        state.value = state.value.copy(columnIndex = newCol)
    }

    private fun moveRow(delta: Int) {
        if (categories.isEmpty()) return
        val newRow = (state.value.rowIndex + delta).mod(categories.size)
        val newColCount = categories[newRow].apps.size
        val newCol = if (WheelConfig.rememberFolderPosition.value) {
            state.value.columnIndex.coerceIn(0, (newColCount - 1).coerceAtLeast(0))
        } else 0
        state.value = WheelUiState(newRow, newCol)
    }

    override fun onCenterTap() {
        val row = categories.getOrNull(state.value.rowIndex) ?: return
        val app = row.apps.getOrNull(state.value.columnIndex) ?: return
        context.startActivity(app.launchIntent)
    }

    override fun onCenterLongPress() { }
}