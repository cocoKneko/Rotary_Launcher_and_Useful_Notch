package com.example.usefulnotch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.example.usefulnotch.ui.theme.RotaryLauncherTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

class notchapp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotchConfigPersistence.load(this)
        observeFoldState()
        setContent {
            RotaryLauncherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermissionGate(modifier = Modifier.padding(innerPadding))
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow {
                    listOf(
                        NotchConfig.widthPx.value,
                        NotchConfig.heightPx.value,
                        NotchConfig.cornerRadiusPx.value,
                        NotchConfig.pillXPx.value,
                        NotchConfig.pillYPx.value,
                        NotchConfig.shape.value.ordinal.toFloat(),
                        NotchConfig.horizontalAnchor.value.ordinal.toFloat(),
                        NotchConfig.screenTarget.value.ordinal.toFloat()
                    )
                }.collect { NotchConfigPersistence.save(this@notchapp) }
            }
        }
    }

    private fun observeFoldState() {
        lifecycleScope.launch {
            WindowInfoTracker.getOrCreate(this@notchapp)
                .windowLayoutInfo(this@notchapp)
                .collect { layoutInfo ->
                    val hasFoldingFeature = layoutInfo.displayFeatures.any { it is FoldingFeature }
                    NotchConfig.isFoldableUnfolded.value = hasFoldingFeature
                    if (!hasFoldingFeature) {
                        NotchConfig.setScreenTarget(ScreenTarget.WHOLE)
                    }
                }
        }
    }
}

@Composable
fun <T> LabeledDropdown(label: String, options: List<T>, current: T, optionLabel: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(140.dp))
        Box {
            Button(onClick = { expanded = true }) {
                Text(optionLabel(current))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFFFF9800) else ButtonDefaults.buttonColors().containerColor
        )
    ) {
        Text(label)
    }
}

@Composable
fun PermissionGate(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasPermission) {
        Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { context.startForegroundService(Intent(context, NotchOverlayService::class.java)) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) { Text("Start") }
                Button(
                    onClick = { context.stopService(Intent(context, NotchOverlayService::class.java)) },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) { Text("Stop") }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                ActiveButton(
                    "Pill", NotchConfig.shape.value == NotchShape.PILL,
                    onClick = { NotchConfig.setShape(NotchShape.PILL) }
                )
                ActiveButton(
                    "Waterfall", NotchConfig.shape.value == NotchShape.WATERFALL,
                    onClick = { NotchConfig.setShape(NotchShape.WATERFALL) }
                )
            }

            DetentDial("Width", NotchConfig.widthPx.value,
                { NotchConfig.widthPx.value = (it.toInt() / 2 * 2).toFloat() }, 60f..600f, step = 2f)
            DetentDial("Height", NotchConfig.heightPx.value,
                { NotchConfig.heightPx.value = it }, 60f..300f)
            DetentDial("Corner radius", NotchConfig.cornerRadiusPx.value,
                { NotchConfig.cornerRadiusPx.value = it }, 0f..150f)

            LabeledDropdown(
                "Screen target",
                listOf(ScreenTarget.WHOLE) + if (NotchConfig.isFoldableUnfolded.value)
                    listOf(ScreenTarget.LEFT_HALF, ScreenTarget.RIGHT_HALF) else emptyList(),
                NotchConfig.screenTarget.value,
                { it.name },
                { NotchConfig.setScreenTarget(it) }
            )
            Text(if (NotchConfig.isFoldableUnfolded.value) "Unfolded (large screen)" else "Folded (cover screen)")

            val validAnchors = if (NotchConfig.shape.value == NotchShape.PILL) {
                listOf(HorizontalAnchor.LEFT, HorizontalAnchor.CENTER, HorizontalAnchor.RIGHT)
            } else {
                NotchConfig.validAnchorsFor(NotchConfig.screenTarget.value)
            }
            LabeledDropdown(
                "Horizontal anchor", validAnchors, NotchConfig.horizontalAnchor.value,
                { it.name }, { NotchConfig.setHorizontalAnchor(it) }
            )

            if (NotchConfig.shape.value == NotchShape.PILL) {
                Button(onClick = {
                    NotchConfig.pillXPx.value = NotchConfig.pillCenterAnchorX(
                        NotchConfig.horizontalAnchor.value, NotchConfig.screenTarget.value,
                        NotchConfig.screenWidthPx.value, NotchConfig.widthPx.value
                    )
                }) { Text("Center") }

                DetentDial("X position", NotchConfig.pillXPx.value,
                    { NotchConfig.pillXPx.value = it }, 0f..NotchConfig.screenWidthPx.value)
                DetentDial("Y position", NotchConfig.pillYPx.value,
                    { NotchConfig.pillYPx.value = it }, 0f..550f)
            }
        }
    } else {
        Button(
            onClick = {
                context.startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ))
            },
            modifier = modifier
        ) { Text("Grant overlay permission") }
    }
}