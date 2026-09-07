package com.example.rotarylauncher

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

enum class WheelDisplayStyle { CENTER, EDGE }

object WheelConfig {
    val wheelSizeDp = mutableFloatStateOf(280f)
    val innerRadiusRatio = mutableFloatStateOf(0.4f)
    val detentsPerRotation = mutableFloatStateOf(24f)
    val tapArcToleranceDeg = mutableFloatStateOf(6f)
    val wheelCenterXDp = mutableFloatStateOf(-1f)
    val wheelCenterYDp = mutableFloatStateOf(-1f)
    val appDisplayIconSizeDp = mutableFloatStateOf(56f)

    val rememberFolderPosition = mutableStateOf(false)
    val dpadVerticalInverted = mutableStateOf(false)

    val appArcCenterYDp = mutableFloatStateOf(140f)
    val appArcRadiusDp = mutableFloatStateOf(90f)
    val appIconAngularGapDeg = mutableFloatStateOf(30f)

    val connectorGradientStartFraction = mutableFloatStateOf(0.3f)
    val connectorLineThicknessDp = mutableFloatStateOf(1.5f)
    val connectorGradientEnabled = mutableStateOf(true)

    val arcMirrored = mutableStateOf(false)
    val displayStyle = mutableStateOf(WheelDisplayStyle.CENTER)
    val debugOverlayEnabled = mutableStateOf(false)

    val topBottomSplitPercent = mutableFloatStateOf(50f)
    val pitchBlackMode = mutableStateOf(false)

    // Pivot positioning — see MainActivity/AppWheelDisplay comments for exact semantics per mode.
    val edgeMarginDp = mutableFloatStateOf(24f)
    val pivotSkewXDp = mutableFloatStateOf(0f)
    val pivotSkewYDp = mutableFloatStateOf(0f)

    // Height of the prev/current/next folder strip within the top display area.
    val folderStripHeightDp = mutableFloatStateOf(50f)
}