package com.example.rotarylauncher

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

enum class WheelDisplayStyle { CENTER, EDGE }
enum class FolderStripPosition { TOP, BOTTOM }
enum class FavoritesPosition { TOP, BOTTOM }

object WheelConfig {
    // ---- Defaults live here. Edit these values directly if you want to change
    // what "Reset to Default" resets to. ----
    private const val DEFAULT_WHEEL_SIZE_DP = 280f
    private const val DEFAULT_INNER_RADIUS_RATIO = 0.4f
    private const val DEFAULT_DETENTS_PER_ROTATION = 24f
    private const val DEFAULT_TAP_ARC_TOLERANCE_DEG = 6f
    private const val DEFAULT_ICON_SIZE_DP = 56f
    private const val DEFAULT_ARC_RADIUS_DP = 90f
    private const val DEFAULT_ANGULAR_GAP_DEG = 30f
    private const val DEFAULT_CONNECTOR_GRADIENT_START = 0.3f
    private const val DEFAULT_CONNECTOR_THICKNESS_DP = 1.5f
    private const val DEFAULT_TOP_BOTTOM_SPLIT_PERCENT = 50f
    private const val DEFAULT_EDGE_MARGIN_DP = 24f
    private const val DEFAULT_FOLDER_STRIP_HEIGHT_DP = 50f
    private const val DEFAULT_FAVORITES_ROW_HEIGHT_DP = 64f
    private const val DEFAULT_MACRO_ROW_HEIGHT_DP = 50f

    val wheelSizeDp = mutableFloatStateOf(DEFAULT_WHEEL_SIZE_DP)
    val innerRadiusRatio = mutableFloatStateOf(DEFAULT_INNER_RADIUS_RATIO)
    val detentsPerRotation = mutableFloatStateOf(DEFAULT_DETENTS_PER_ROTATION)
    val tapArcToleranceDeg = mutableFloatStateOf(DEFAULT_TAP_ARC_TOLERANCE_DEG)

    // Normalized -1..1, 0 = dead center of the wheel's containing area.
    val wheelCenterXNorm = mutableFloatStateOf(0f)
    val wheelCenterYNorm = mutableFloatStateOf(0f)
    // Legacy absolute-dp fields — still used internally as the actual render offset,
    // computed FROM the normalized sliders now rather than set directly.
    val wheelCenterXDp = mutableFloatStateOf(-1f)
    val wheelCenterYDp = mutableFloatStateOf(-1f)

    val appDisplayIconSizeDp = mutableFloatStateOf(DEFAULT_ICON_SIZE_DP)

    val rememberFolderPosition = mutableStateOf(false)
    val dpadVerticalInverted = mutableStateOf(false)

    val appArcRadiusDp = mutableFloatStateOf(DEFAULT_ARC_RADIUS_DP)
    val appIconAngularGapDeg = mutableFloatStateOf(DEFAULT_ANGULAR_GAP_DEG)

    val connectorGradientStartFraction = mutableFloatStateOf(DEFAULT_CONNECTOR_GRADIENT_START)
    val connectorLineThicknessDp = mutableFloatStateOf(DEFAULT_CONNECTOR_THICKNESS_DP)
    val connectorGradientEnabled = mutableStateOf(true)

    val arcMirrored = mutableStateOf(false)
    val displayStyle = mutableStateOf(WheelDisplayStyle.CENTER)
    val debugOverlayEnabled = mutableStateOf(false)

    val topBottomSplitPercent = mutableFloatStateOf(DEFAULT_TOP_BOTTOM_SPLIT_PERCENT)
    val pitchBlackMode = mutableStateOf(false)

    val edgeMarginDp = mutableFloatStateOf(DEFAULT_EDGE_MARGIN_DP)
    val pivotSkewXDp = mutableFloatStateOf(0f)
    val pivotSkewYDp = mutableFloatStateOf(0f)

    val folderStripHeightDp = mutableFloatStateOf(DEFAULT_FOLDER_STRIP_HEIGHT_DP)
    val folderStripPosition = mutableStateOf(FolderStripPosition.BOTTOM)

    val favoritesRowHeightDp = mutableFloatStateOf(DEFAULT_FAVORITES_ROW_HEIGHT_DP)
    val favoritesPosition = mutableStateOf(FavoritesPosition.TOP)
    val macroRowHeightDp = mutableFloatStateOf(DEFAULT_MACRO_ROW_HEIGHT_DP)

    fun resetLayoutToDefaults() {
        wheelSizeDp.floatValue = DEFAULT_WHEEL_SIZE_DP
        innerRadiusRatio.floatValue = DEFAULT_INNER_RADIUS_RATIO
        detentsPerRotation.floatValue = DEFAULT_DETENTS_PER_ROTATION
        tapArcToleranceDeg.floatValue = DEFAULT_TAP_ARC_TOLERANCE_DEG
        wheelCenterXNorm.floatValue = 0f
        wheelCenterYNorm.floatValue = 0f
        appDisplayIconSizeDp.floatValue = DEFAULT_ICON_SIZE_DP
        appArcRadiusDp.floatValue = DEFAULT_ARC_RADIUS_DP
        appIconAngularGapDeg.floatValue = DEFAULT_ANGULAR_GAP_DEG
        connectorGradientStartFraction.floatValue = DEFAULT_CONNECTOR_GRADIENT_START
        connectorLineThicknessDp.floatValue = DEFAULT_CONNECTOR_THICKNESS_DP
        topBottomSplitPercent.floatValue = DEFAULT_TOP_BOTTOM_SPLIT_PERCENT
        edgeMarginDp.floatValue = DEFAULT_EDGE_MARGIN_DP
        pivotSkewXDp.floatValue = 0f
        pivotSkewYDp.floatValue = 0f
        folderStripHeightDp.floatValue = DEFAULT_FOLDER_STRIP_HEIGHT_DP
        favoritesRowHeightDp.floatValue = DEFAULT_FAVORITES_ROW_HEIGHT_DP
        macroRowHeightDp.floatValue = DEFAULT_MACRO_ROW_HEIGHT_DP
    }
}