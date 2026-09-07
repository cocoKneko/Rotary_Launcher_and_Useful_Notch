package com.example.rotarylauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rotarylauncher.ui.theme.LabelStyle
import com.example.rotarylauncher.ui.theme.RotaryLauncherTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val HAIRLINE_ALPHA = 0.2f
private const val PRESS_OVERLAY_ALPHA = 0.10f

private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val expected = "${context.packageName}/${serviceClass.name}"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    while (splitter.hasNext()) if (splitter.next().equals(expected, ignoreCase = true)) return true
    return false
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigPersistence.load(this)
        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = AppSettingsState.darkModeOverride.value ?: systemDark

            RotaryLauncherTheme(darkTheme = darkTheme, pitchBlack = WheelConfig.pitchBlackMode.value) {
                val context = LocalContext.current
                val configuration = LocalConfiguration.current

                var accessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context, LockAccessibilityService::class.java)) }

                BackHandler(enabled = AppSettingsState.showSettings.value) {
                    AppSettingsState.showSettings.value = false
                }

                val allDetectedApps = remember { detectInstalledApps(context.packageManager) }
                LaunchedEffect(Unit) { FolderPersistence.load(context, allDetectedApps) }

                val categories = FoldersConfig.folders.value
                    .filter { it.appPackageNames.isNotEmpty() }
                    .map { folder ->
                        Category(name = folder.name, apps = folder.appPackageNames.mapNotNull { pkg -> allDetectedApps.find { it.packageName == pkg } })
                    }

                LaunchedEffect(
                    FoldersConfig.folders.value, FavoritesConfig.slot1.value, FavoritesConfig.slot2.value,
                    FavoritesConfig.slot3.value, FavoritesConfig.slot4.value
                ) { FolderPersistence.save(context) }

                val wheelState = remember { mutableStateOf(WheelUiState()) }
                val inputHandler = remember(categories) { HomeInputHandler(categories, wheelState, context) }

                LaunchedEffect(categories.size) {
                    if (categories.isNotEmpty()) {
                        val safeRow = wheelState.value.rowIndex.coerceIn(0, categories.size - 1)
                        val safeCol = wheelState.value.columnIndex.coerceIn(0, (categories.getOrNull(safeRow)?.apps?.size ?: 1) - 1).coerceAtLeast(0)
                        if (safeRow != wheelState.value.rowIndex || safeCol != wheelState.value.columnIndex) {
                            wheelState.value = WheelUiState(safeRow, safeCol)
                        }
                    }
                }

                val hapticScope = rememberCoroutineScope()
                fun buttonHaptic() = fireHaptic(context, hapticScope, HapticsConfig.buttonIntensityMs.floatValue, HapticsConfig.buttonDelayMs.floatValue)
                fun detentHaptic() = fireHaptic(context, hapticScope, HapticsConfig.detentIntensityMs.floatValue, HapticsConfig.detentDelayMs.floatValue)

                LaunchedEffect(Unit) {
                    if (WheelConfig.wheelCenterXDp.floatValue < 0f) WheelConfig.wheelCenterXDp.floatValue = configuration.screenWidthDp / 2f
                    if (WheelConfig.wheelCenterYDp.floatValue < 0f) WheelConfig.wheelCenterYDp.floatValue = configuration.screenHeightDp / 2f
                }

                LaunchedEffect(Unit) {
                    snapshotFlow {
                        listOf(
                            WheelConfig.wheelSizeDp.floatValue, WheelConfig.innerRadiusRatio.floatValue,
                            WheelConfig.detentsPerRotation.floatValue, WheelConfig.tapArcToleranceDeg.floatValue,
                            WheelConfig.wheelCenterXDp.floatValue, WheelConfig.wheelCenterYDp.floatValue,
                            WheelConfig.appArcCenterYDp.floatValue, WheelConfig.appArcRadiusDp.floatValue,
                            WheelConfig.appDisplayIconSizeDp.floatValue, WheelConfig.appIconAngularGapDeg.floatValue,
                            WheelConfig.connectorGradientStartFraction.floatValue, WheelConfig.connectorLineThicknessDp.floatValue,
                            WheelConfig.topBottomSplitPercent.floatValue, WheelConfig.edgeMarginDp.floatValue,
                            WheelConfig.pivotSkewXDp.floatValue, WheelConfig.pivotSkewYDp.floatValue,
                            WheelConfig.folderStripHeightDp.floatValue,
                            HapticsConfig.buttonIntensityMs.floatValue, HapticsConfig.detentIntensityMs.floatValue,
                            HapticsConfig.buttonDelayMs.floatValue, HapticsConfig.detentDelayMs.floatValue
                        )
                    }.collect { ConfigPersistence.save(context) }
                }

                var centerPressed by remember { mutableStateOf(false) }
                var upPressed by remember { mutableStateOf(false) }
                var downPressed by remember { mutableStateOf(false) }
                var leftPressed by remember { mutableStateOf(false) }
                var rightPressed by remember { mutableStateOf(false) }
                var isRotatingDrag by remember { mutableStateOf(false) }
                var detentCount by remember { mutableIntStateOf(0) }
                var eventLog by remember { mutableStateOf("Waiting For Input...") }
                var wheelScrollProgress by remember { mutableFloatStateOf(0f) }
                var wheelAngleDeg by remember { mutableFloatStateOf(0f) }

                val idleColor = MaterialTheme.colorScheme.surface
                val accent = MaterialTheme.colorScheme.primary
                val onSurface = MaterialTheme.colorScheme.onSurface
                val onBg = MaterialTheme.colorScheme.onBackground

                fun setZone(zone: DpadDirection, pressed: Boolean) {
                    when (zone) {
                        DpadDirection.UP -> upPressed = pressed
                        DpadDirection.DOWN -> downPressed = pressed
                        DpadDirection.LEFT -> leftPressed = pressed
                        DpadDirection.RIGHT -> rightPressed = pressed
                    }
                }

                val detentThresholdDeg = 360f / WheelConfig.detentsPerRotation.floatValue

                fun lockScreen() {
                    LockAccessibilityService.instance?.lock()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding()
                        .pointerInput(Unit) { detectTapGestures(onDoubleTap = { lockScreen() }) }
                ) {
                    if (AppSettingsState.showSettings.value) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { AppSettingsState.showSettings.value = false }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = onBg)
                                }
                                Spacer(Modifier.width(8.dp))
                                SettingsTabLabel("Tuning", AppSettingsState.settingsTab.value == SettingsTab.TUNING, onBg) { AppSettingsState.settingsTab.value = SettingsTab.TUNING }
                                Spacer(Modifier.width(16.dp))
                                SettingsTabLabel("Apps", AppSettingsState.settingsTab.value == SettingsTab.APPS, onBg) { AppSettingsState.settingsTab.value = SettingsTab.APPS }
                            }

                            if (AppSettingsState.settingsTab.value == SettingsTab.APPS) {
                                Box(modifier = Modifier.weight(1f)) { AppManagerScreen(allApps = allDetectedApps) }
                            } else {
                                Column(modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState())) {
                                    Text("General", color = accent, style = LabelStyle)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Follow System Theme: ${if (AppSettingsState.darkModeOverride.value == null) "ON" else "OFF"}", color = onBg, style = LabelStyle)
                                        Switch(checked = AppSettingsState.darkModeOverride.value == null, onCheckedChange = { AppSettingsState.darkModeOverride.value = if (it) null else systemDark })
                                    }
                                    if (AppSettingsState.darkModeOverride.value != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Theme: ${if (AppSettingsState.darkModeOverride.value == true) "Dark" else "Light"}", color = onBg, style = LabelStyle)
                                            Switch(checked = AppSettingsState.darkModeOverride.value == true, onCheckedChange = { AppSettingsState.darkModeOverride.value = it })
                                        }
                                    }
                                    if (darkTheme) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Pitch Black Mode: ${if (WheelConfig.pitchBlackMode.value) "ON" else "OFF"}", color = onBg, style = LabelStyle)
                                            Switch(checked = WheelConfig.pitchBlackMode.value, onCheckedChange = { WheelConfig.pitchBlackMode.value = it; ConfigPersistence.save(context) })
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Remember Folder Position: ${if (WheelConfig.rememberFolderPosition.value) "ON" else "OFF"}", color = onBg, style = LabelStyle)
                                        Switch(checked = WheelConfig.rememberFolderPosition.value, onCheckedChange = { WheelConfig.rememberFolderPosition.value = it })
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Invert Up/Down: ${if (WheelConfig.dpadVerticalInverted.value) "ON" else "OFF"}", color = onBg, style = LabelStyle)
                                        Switch(checked = WheelConfig.dpadVerticalInverted.value, onCheckedChange = { WheelConfig.dpadVerticalInverted.value = it })
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Debug Overlay: ${if (WheelConfig.debugOverlayEnabled.value) "ON" else "OFF"}", color = onBg, style = LabelStyle)
                                        Switch(checked = WheelConfig.debugOverlayEnabled.value, onCheckedChange = { WheelConfig.debugOverlayEnabled.value = it; ConfigPersistence.save(context) })
                                    }
                                    if (!accessibilityEnabled) {
                                        Button(onClick = {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }) { Text("Enable Double-Tap Lock (Accessibility)", style = LabelStyle) }
                                    } else {
                                        Text("Double-Tap Lock: ENABLED", color = onBg, style = LabelStyle)
                                    }
                                    Button(onClick = { accessibilityEnabled = isAccessibilityServiceEnabled(context, LockAccessibilityService::class.java) }) {
                                        Text("Recheck Accessibility Status", style = LabelStyle)
                                    }
                                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) {
                                        Text("Change Default Launcher", style = LabelStyle)
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Text("Layout", color = accent, style = LabelStyle)
                                    DetentDial("Top/Bottom split (%)", WheelConfig.topBottomSplitPercent.floatValue, { WheelConfig.topBottomSplitPercent.floatValue = it }, 0f..100f)
                                    DetentDial("Folder strip height (dp)", WheelConfig.folderStripHeightDp.floatValue, { WheelConfig.folderStripHeightDp.floatValue = it }, 0f..150f)
                                    Button(onClick = { WheelConfig.displayStyle.value = if (WheelConfig.displayStyle.value == WheelDisplayStyle.CENTER) WheelDisplayStyle.EDGE else WheelDisplayStyle.CENTER; ConfigPersistence.save(context) }) {
                                        Text("Display Style: ${WheelConfig.displayStyle.value.name}", style = LabelStyle)
                                    }
                                    Button(onClick = { WheelConfig.arcMirrored.value = !WheelConfig.arcMirrored.value; ConfigPersistence.save(context) }) {
                                        Text("Layout: ${if (WheelConfig.arcMirrored.value) "FLIPPED" else "NORMAL"}", style = LabelStyle)
                                    }
                                    DetentDial("Edge margin (dp)", WheelConfig.edgeMarginDp.floatValue, { WheelConfig.edgeMarginDp.floatValue = it }, 0f..(configuration.screenWidthDp / 2f))
                                    DetentDial("Pivot skew X (dp)", WheelConfig.pivotSkewXDp.floatValue, { WheelConfig.pivotSkewXDp.floatValue = it }, -300f..300f)
                                    DetentDial("Pivot skew Y (dp)", WheelConfig.pivotSkewYDp.floatValue, { WheelConfig.pivotSkewYDp.floatValue = it }, -300f..300f)

                                    Spacer(Modifier.height(12.dp))
                                    Text("iPod Wheel", color = accent, style = LabelStyle)
                                    DetentDial("Wheel size (dp)", WheelConfig.wheelSizeDp.floatValue, { WheelConfig.wheelSizeDp.floatValue = it }, 150f..350f)
                                    DetentDial("Inner button ratio (x100)", WheelConfig.innerRadiusRatio.floatValue * 100f, { WheelConfig.innerRadiusRatio.floatValue = it / 100f }, 10f..60f)
                                    DetentDial("Detents per rotation", WheelConfig.detentsPerRotation.floatValue, { WheelConfig.detentsPerRotation.floatValue = it }, 1f..100f)
                                    DetentDial("Tap deadzone (deg)", WheelConfig.tapArcToleranceDeg.floatValue, { WheelConfig.tapArcToleranceDeg.floatValue = it }, 1f..20f)
                                    DetentDial("Wheel center X (dp)", WheelConfig.wheelCenterXDp.floatValue, { WheelConfig.wheelCenterXDp.floatValue = it }, 0f..configuration.screenWidthDp.toFloat())
                                    Button(onClick = { WheelConfig.wheelCenterXDp.floatValue = configuration.screenWidthDp / 2f }) { Text("Center X", style = LabelStyle) }
                                    DetentDial("Wheel center Y (dp)", WheelConfig.wheelCenterYDp.floatValue, { WheelConfig.wheelCenterYDp.floatValue = it }, 0f..configuration.screenHeightDp.toFloat())

                                    Spacer(Modifier.height(12.dp))
                                    Text("App List / Circle", color = accent, style = LabelStyle)
                                    DetentDial("App icon size (dp)", WheelConfig.appDisplayIconSizeDp.floatValue, { WheelConfig.appDisplayIconSizeDp.floatValue = it }, 1f..150f)
                                    DetentDial("Angular gap (deg)", WheelConfig.appIconAngularGapDeg.floatValue, { WheelConfig.appIconAngularGapDeg.floatValue = it }, 5f..90f)
                                    DetentDial("Arc radius (dp)", WheelConfig.appArcRadiusDp.floatValue, { WheelConfig.appArcRadiusDp.floatValue = it }, 1f..1000f)
                                    DetentDial("Connector gradient start (x100)", WheelConfig.connectorGradientStartFraction.floatValue * 100f, { WheelConfig.connectorGradientStartFraction.floatValue = it / 100f }, 0f..99f)
                                    DetentDial("Connector line thickness (dp)", WheelConfig.connectorLineThicknessDp.floatValue, { WheelConfig.connectorLineThicknessDp.floatValue = it }, 0.5f..10f)
                                    Button(onClick = { WheelConfig.connectorGradientEnabled.value = !WheelConfig.connectorGradientEnabled.value; ConfigPersistence.save(context) }) {
                                        Text("Connector Gradient: ${if (WheelConfig.connectorGradientEnabled.value) "ON" else "OFF"}", style = LabelStyle)
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Text("Haptics", color = accent, style = LabelStyle)
                                    DetentDial("Button haptic intensity (ms)", HapticsConfig.buttonIntensityMs.floatValue, { HapticsConfig.buttonIntensityMs.floatValue = it }, 1f..100f)
                                    DetentDial("Detent haptic intensity (ms)", HapticsConfig.detentIntensityMs.floatValue, { HapticsConfig.detentIntensityMs.floatValue = it }, 1f..100f)
                                    DetentDial("Button haptic delay (ms)", HapticsConfig.buttonDelayMs.floatValue, { HapticsConfig.buttonDelayMs.floatValue = it }, 0f..200f)
                                    DetentDial("Detent haptic delay (ms)", HapticsConfig.detentDelayMs.floatValue, { HapticsConfig.detentDelayMs.floatValue = it }, 0f..200f)
                                }
                            }

                            Button(onClick = { AppSettingsState.showSettings.value = false }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Back", style = LabelStyle) }
                        }
                    } else if (categories.isEmpty()) {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Folders With Apps Yet", color = onBg, style = LabelStyle)
                            Text("Tap The Gear To Assign Apps", color = onBg.copy(alpha = 0.5f), style = LabelStyle)
                        }
                    } else {
                        val splitFraction = (WheelConfig.topBottomSplitPercent.floatValue / 100f).coerceIn(0.001f, 0.999f)

                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(splitFraction).fillMaxWidth()) {
                                AppWheelDisplay(
                                    categories = categories, wheelState = wheelState.value, onBg = onBg, accent = accent,
                                    mutedColor = onSurface.copy(alpha = 0.35f), wheelScrollProgress = wheelScrollProgress, isRotatingDrag = isRotatingDrag
                                )
                            }

                            Box(modifier = Modifier.weight(1f - splitFraction).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    FavoritesConfig.slots().forEachIndexed { index, slot ->
                                        val assigned = slot.value != null
                                        Box(
                                            modifier = Modifier.size(42.dp).clip(CircleShape).border(1.dp, onSurface.copy(alpha = HAIRLINE_ALPHA), CircleShape)
                                                .then(if (assigned) Modifier.clickable { allDetectedApps.find { it.packageName == slot.value }?.let { context.startActivity(it.launchIntent) } } else Modifier),
                                            contentAlignment = Alignment.Center
                                        ) { Text("${index + 1}", color = onSurface.copy(alpha = 0.4f), style = LabelStyle) }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = (WheelConfig.wheelCenterXDp.floatValue - WheelConfig.wheelSizeDp.floatValue / 2f).dp,
                                            y = (WheelConfig.wheelCenterYDp.floatValue - WheelConfig.wheelSizeDp.floatValue / 2f).dp
                                        )
                                        .size(WheelConfig.wheelSizeDp.floatValue.dp)
                                        .then(if (WheelConfig.debugOverlayEnabled.value) Modifier.border(1.dp, Color.Red.copy(alpha = 0.5f)) else Modifier)
                                        .pointerInput(WheelConfig.wheelSizeDp.floatValue, WheelConfig.innerRadiusRatio.floatValue, detentThresholdDeg, WheelConfig.tapArcToleranceDeg.floatValue) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown()
                                                val center = Offset(size.width / 2f, size.height / 2f)
                                                val outerRadius = size.width / 2f
                                                val innerRadius = outerRadius * WheelConfig.innerRadiusRatio.floatValue
                                                val distFromCenter = hypot(down.position.x - center.x, down.position.y - center.y)

                                                centerPressed = false; upPressed = false; downPressed = false; leftPressed = false; rightPressed = false
                                                isRotatingDrag = false
                                                detentCount = 0
                                                wheelScrollProgress = 0f
                                                if (distFromCenter > outerRadius) return@awaitEachGesture

                                                if (distFromCenter < innerRadius) {
                                                    centerPressed = true
                                                    buttonHaptic()
                                                    eventLog = "Center: Pressed"
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                        if (!change.pressed) {
                                                            centerPressed = false
                                                            buttonHaptic()
                                                            inputHandler.onCenterTap()
                                                            eventLog = "Center: Tapped"
                                                            break
                                                        }
                                                    }
                                                } else {
                                                    val startAngle = angleTo(down.position, center)
                                                    val zone = zoneForAngle(startAngle)
                                                    setZone(zone, true)
                                                    buttonHaptic()
                                                    eventLog = "Donut: Pressed ($zone)"

                                                    var lastAngle = startAngle
                                                    var rotationAccum = 0f
                                                    var totalArcTravel = 0f

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                                        if (change.pressed) {
                                                            val angle = angleTo(change.position, center)
                                                            var delta = angle - lastAngle
                                                            if (delta > 180f) delta -= 360f
                                                            if (delta < -180f) delta += 360f
                                                            rotationAccum += delta
                                                            totalArcTravel += abs(delta)
                                                            lastAngle = angle
                                                            wheelAngleDeg = (wheelAngleDeg + delta + 360f) % 360f

                                                            if (!isRotatingDrag && totalArcTravel > WheelConfig.tapArcToleranceDeg.floatValue) {
                                                                isRotatingDrag = true
                                                                setZone(zone, false)
                                                            }

                                                            if (isRotatingDrag) {
                                                                // Direction now depends on mirror state — this is the
                                                                // fix for both the reversed-rotation and jarring-snap bugs.
                                                                val sign = if (WheelConfig.arcMirrored.value) -1 else 1
                                                                while (rotationAccum >= detentThresholdDeg) {
                                                                    inputHandler.onRotate(sign)
                                                                    rotationAccum -= detentThresholdDeg
                                                                    detentCount++
                                                                    detentHaptic()
                                                                    eventLog = "Rotate CW ($detentCount)"
                                                                }
                                                                while (rotationAccum <= -detentThresholdDeg) {
                                                                    inputHandler.onRotate(-sign)
                                                                    rotationAccum += detentThresholdDeg
                                                                    detentCount--
                                                                    detentHaptic()
                                                                    eventLog = "Rotate CCW ($detentCount)"
                                                                }
                                                                wheelScrollProgress = rotationAccum / detentThresholdDeg
                                                            }
                                                            change.consume()
                                                        } else {
                                                            buttonHaptic()
                                                            if (isRotatingDrag) {
                                                                isRotatingDrag = false
                                                                eventLog = "Donut: Released ($detentCount)"
                                                            } else {
                                                                setZone(zone, false)
                                                                inputHandler.onDpad(zone)
                                                                eventLog = "DPad $zone Tapped"
                                                            }
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    val centerAlpha by animateFloatAsState(if (centerPressed) PRESS_OVERLAY_ALPHA else 0f, tween(150), label = "centerA")
                                    val upAlpha by animateFloatAsState(if (upPressed) PRESS_OVERLAY_ALPHA else 0f, tween(150), label = "upA")
                                    val downAlpha by animateFloatAsState(if (downPressed) PRESS_OVERLAY_ALPHA else 0f, tween(150), label = "downA")
                                    val leftAlpha by animateFloatAsState(if (leftPressed) PRESS_OVERLAY_ALPHA else 0f, tween(150), label = "leftA")
                                    val rightAlpha by animateFloatAsState(if (rightPressed) PRESS_OVERLAY_ALPHA else 0f, tween(150), label = "rightA")
                                    val dotAlpha by animateFloatAsState(if (isRotatingDrag) 1f else 0f, tween(150), label = "dotA")

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val outerRadius = size.width / 2f
                                        val innerRadius = outerRadius * WheelConfig.innerRadiusRatio.floatValue
                                        val midRadius = (innerRadius + outerRadius) / 2f
                                        val ringThickness = outerRadius - innerRadius

                                        drawCircle(color = idleColor, radius = outerRadius, center = center)

                                        fun drawZoneOverlay(startAngle: Float, alpha: Float) {
                                            if (alpha <= 0f) return
                                            drawArc(
                                                color = onSurface.copy(alpha = alpha), startAngle = startAngle, sweepAngle = 90f, useCenter = false,
                                                topLeft = Offset(center.x - midRadius, center.y - midRadius), size = Size(midRadius * 2f, midRadius * 2f), style = Stroke(width = ringThickness)
                                            )
                                        }
                                        drawZoneOverlay(-45f, rightAlpha); drawZoneOverlay(45f, downAlpha); drawZoneOverlay(135f, leftAlpha); drawZoneOverlay(225f, upAlpha)

                                        if (centerAlpha > 0f) drawCircle(color = onSurface.copy(alpha = centerAlpha), radius = innerRadius, center = center)

                                        val hairline = onSurface.copy(alpha = HAIRLINE_ALPHA)
                                        drawCircle(color = hairline, radius = outerRadius, center = center, style = Stroke(width = 1.5f))
                                        drawCircle(color = hairline, radius = innerRadius, center = center, style = Stroke(width = 1.5f))

                                        // Position dot — restored.
                                        val rad = Math.toRadians(wheelAngleDeg.toDouble())
                                        val dotDistance = outerRadius + 12f
                                        val dotCenter = Offset(center.x + dotDistance * cos(rad).toFloat(), center.y + dotDistance * sin(rad).toFloat())
                                        drawCircle(color = hairline, radius = 6f, center = dotCenter)
                                        if (dotAlpha > 0f) drawCircle(color = accent.copy(alpha = dotAlpha), radius = 6f, center = dotCenter)
                                    }
                                }
                            }
                        }

                        if (WheelConfig.debugOverlayEnabled.value) {
                            val row = categories[wheelState.value.rowIndex]
                            val app = row.apps.getOrNull(wheelState.value.columnIndex)
                            Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                                Text("${row.name} > ${app?.label ?: "—"}", color = onBg, style = LabelStyle)
                                Text(eventLog, color = onBg, style = LabelStyle)
                                if (detentCount != 0) Text("Detents: $detentCount", color = accent, style = LabelStyle)
                            }
                        }
                    }

                    if (!AppSettingsState.showSettings.value) {
                        SmallFloatingActionButton(
                            onClick = { AppSettingsState.showSettings.value = true },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = onBg,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).border(1.dp, onBg.copy(alpha = HAIRLINE_ALPHA), CircleShape)
                        ) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                    }
                }
            }
        }
    }
}

private fun angleTo(p: Offset, center: Offset): Float {
    val dx = p.x - center.x; val dy = p.y - center.y
    return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
}

private fun zoneForAngle(angleDeg: Float): DpadDirection {
    val a = (angleDeg + 360f) % 360f
    return when {
        a >= 315f || a < 45f -> DpadDirection.RIGHT
        a in 45f..134.999f -> DpadDirection.DOWN
        a in 135f..224.999f -> DpadDirection.LEFT
        else -> DpadDirection.UP
    }
}

@Composable
private fun SettingsTabLabel(label: String, selected: Boolean, onBg: Color, onClick: () -> Unit) {
    Text(label, color = if (selected) onBg else onBg.copy(alpha = 0.4f), style = LabelStyle, modifier = Modifier.clickable(onClick = onClick))
}