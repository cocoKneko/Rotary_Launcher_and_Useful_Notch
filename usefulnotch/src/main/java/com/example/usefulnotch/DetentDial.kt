package com.example.usefulnotch

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DetentDial(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    pixelsPerStep: Float = 20f,
    accentColor: Color = Color(0xFF333333)
) {
    val currentValue = rememberUpdatedState(value)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ${value.toInt()}", color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(accentColor)
                .pointerInput(Unit) {
                    var workingValue = 0f
                    var dragAccum = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            workingValue = currentValue.value
                            dragAccum = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccum += dragAmount
                            while (dragAccum >= pixelsPerStep) {
                                workingValue = (workingValue + step).coerceIn(valueRange)
                                onValueChange(workingValue)
                                dragAccum -= pixelsPerStep
                            }
                            while (dragAccum <= -pixelsPerStep) {
                                workingValue = (workingValue - step).coerceIn(valueRange)
                                onValueChange(workingValue)
                                dragAccum += pixelsPerStep
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("◀  drag to adjust  ▶", color = Color.White)
        }
    }
}