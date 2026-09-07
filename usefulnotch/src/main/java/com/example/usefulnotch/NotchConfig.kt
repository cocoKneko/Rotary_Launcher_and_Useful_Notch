package com.example.usefulnotch

import androidx.compose.runtime.mutableStateOf

enum class NotchShape { PILL, WATERFALL }
enum class HorizontalAnchor { LEFT, CENTER, RIGHT }
enum class ScreenTarget { WHOLE, LEFT_HALF, RIGHT_HALF }

object NotchConfig {
    val widthPx = mutableStateOf(110f)
    val heightPx = mutableStateOf(110f)
    val cornerRadiusPx = mutableStateOf(55f)
    val shape = mutableStateOf(NotchShape.PILL)
    val horizontalAnchor = mutableStateOf(HorizontalAnchor.CENTER)
    val pillXPx = mutableStateOf(400f) // meaning depends on horizontalAnchor — see pillLeftEdge()
    val pillYPx = mutableStateOf(200f)
    val screenWidthPx = mutableStateOf(1080f)
    val screenTarget = mutableStateOf(ScreenTarget.WHOLE)
    val isFoldableUnfolded = mutableStateOf(false)

    // Waterfall only: which anchors make sense for a given target region.
    fun validAnchorsFor(target: ScreenTarget): List<HorizontalAnchor> = when (target) {
        ScreenTarget.WHOLE -> listOf(HorizontalAnchor.LEFT, HorizontalAnchor.CENTER, HorizontalAnchor.RIGHT)
        ScreenTarget.LEFT_HALF -> listOf(HorizontalAnchor.LEFT, HorizontalAnchor.CENTER)
        ScreenTarget.RIGHT_HALF -> listOf(HorizontalAnchor.CENTER, HorizontalAnchor.RIGHT)
    }

    fun regionFor(target: ScreenTarget, screenWidth: Float): Pair<Float, Float> = when (target) {
        ScreenTarget.WHOLE -> 0f to screenWidth
        ScreenTarget.LEFT_HALF -> 0f to (screenWidth / 2f)
        ScreenTarget.RIGHT_HALF -> (screenWidth / 2f) to screenWidth
    }

    // Waterfall: flush-edge positioning, recomputed fresh every time from anchor+target.
    fun waterfallLeftEdge(anchor: HorizontalAnchor, target: ScreenTarget, screenWidth: Float, shapeWidth: Float): Float {
        val (regionLeft, regionRight) = regionFor(target, screenWidth)
        val regionWidth = regionRight - regionLeft
        return when (anchor) {
            HorizontalAnchor.LEFT -> regionLeft
            HorizontalAnchor.CENTER -> regionLeft + (regionWidth - shapeWidth) / 2f
            HorizontalAnchor.RIGHT -> regionRight - shapeWidth
        }
    }

    // Pill: pillXPx is the FIXED POINT's coordinate, not the window's left edge.
    // This converts (fixed point + current width) into the actual left edge, so the
    // fixed point never moves when width changes — only the opposite side does.
    fun pillLeftEdge(anchorX: Float, anchor: HorizontalAnchor, width: Float): Float = when (anchor) {
        HorizontalAnchor.LEFT -> anchorX
        HorizontalAnchor.CENTER -> anchorX - width / 2f
        HorizontalAnchor.RIGHT -> anchorX - width
    }

    // Used only by the "Center" button: what pillXPx should become so the shape
    // sits visually centered in the selected target region, for the current anchor.
    fun pillCenterAnchorX(anchor: HorizontalAnchor, target: ScreenTarget, screenWidth: Float, shapeWidth: Float): Float {
        val (regionLeft, regionRight) = regionFor(target, screenWidth)
        val regionCenter = (regionLeft + regionRight) / 2f
        return when (anchor) {
            HorizontalAnchor.LEFT -> regionCenter - shapeWidth / 2f
            HorizontalAnchor.CENTER -> regionCenter
            HorizontalAnchor.RIGHT -> regionCenter + shapeWidth / 2f
        }
    }

    fun setShape(newShape: NotchShape) {
        shape.value = newShape
    }

    fun setScreenTarget(newTarget: ScreenTarget) {
        screenTarget.value = newTarget
        if (shape.value == NotchShape.WATERFALL && horizontalAnchor.value !in validAnchorsFor(newTarget)) {
            horizontalAnchor.value = HorizontalAnchor.CENTER
        }
    }

    fun setHorizontalAnchor(newAnchor: HorizontalAnchor) {
        horizontalAnchor.value = newAnchor
        // Deliberately does not move pillXPx — changing anchor only changes which
        // point stays fixed on the NEXT resize, not the current position.
    }
}