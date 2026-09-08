package com.example.rotarylauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size

private fun lerp3(d: Float, v0: Float, v1: Float, v2: Float): Float {
    val dd = d.coerceIn(0f, 2f)
    return if (dd <= 1f) v0 + (v1 - v0) * dd else v1 + (v2 - v1) * (dd - 1f)
}

fun String.capitalizeFirstOnly(): String {
    if (isEmpty()) return this
    return this[0].uppercaseChar() + substring(1)
}

@Composable
private fun SingleLineClip(text: String, style: TextStyle, maxWidthDp: Dp) {
    Box(modifier = Modifier.width(maxWidthDp)) {
        Text(text, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun GradientFadeText(text: String, style: TextStyle, maxWidthDp: Dp, baseAlpha: Float, fadeToStart: Boolean) {
    val stops = if (fadeToStart) arrayOf(0f to Color.Transparent, 1f to Color.Black)
    else arrayOf(0f to Color.Black, 1f to Color.Transparent)
    Box(
        modifier = Modifier
            .width(maxWidthDp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(brush = Brush.horizontalGradient(colorStops = stops), blendMode = BlendMode.DstIn)
            }
    ) {
        Text(text, style = style.copy(color = style.color.copy(alpha = baseAlpha)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun FolderStrip(currentFolder: Category, prevFolder: Category?, nextFolder: Category?, categoriesSize: Int, onBg: Color, accent: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(WheelConfig.folderStripHeightDp.floatValue.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (prevFolder != null && categoriesSize > 1) {
                GradientFadeText(
                    text = prevFolder.name.capitalizeFirstOnly(),
                    style = TextStyle(color = onBg, fontSize = 17.sp, fontWeight = FontWeight.Light),
                    maxWidthDp = 100.dp, baseAlpha = 0.5f, fadeToStart = true
                )
                Box(modifier = Modifier.width(16.dp))
            }
            Text(
                currentFolder.name.capitalizeFirstOnly(),
                color = accent, fontSize = 17.sp, fontWeight = FontWeight.Light,
                maxLines = 1, overflow = TextOverflow.Clip, softWrap = false
            )
            if (nextFolder != null && categoriesSize > 1) {
                Box(modifier = Modifier.width(16.dp))
                GradientFadeText(
                    text = nextFolder.name.capitalizeFirstOnly(),
                    style = TextStyle(color = onBg, fontSize = 17.sp, fontWeight = FontWeight.Light),
                    maxWidthDp = 100.dp, baseAlpha = 0.5f, fadeToStart = false
                )
            }
        }
    }
}

@Composable
fun AppWheelDisplay(
    categories: List<Category>,
    wheelState: WheelUiState,
    onBg: Color,
    accent: Color,
    mutedColor: Color,
    wheelScrollProgress: Float,
    isRotatingDrag: Boolean
) {
    val currentFolder = categories[wheelState.rowIndex]
    val prevFolder = if (categories.size > 1) categories[(wheelState.rowIndex - 1 + categories.size) % categories.size] else null
    val nextFolder = if (categories.size > 1) categories[(wheelState.rowIndex + 1) % categories.size] else null
    val apps = currentFolder.apps
    val density = LocalDensity.current

    val radiusPx = with(density) { WheelConfig.appArcRadiusDp.floatValue.dp.toPx() }
    val angularGapRad = Math.toRadians(WheelConfig.appIconAngularGapDeg.floatValue.toDouble()).toFloat()
    val fullIconSizeDp = WheelConfig.appDisplayIconSizeDp.floatValue
    val mirror = if (WheelConfig.arcMirrored.value) -1f else 1f
    val debug = WheelConfig.debugOverlayEnabled.value
    val ringRadiusPx = with(density) { 8.dp.toPx() }
    val edgeMarginPx = with(density) { WheelConfig.edgeMarginDp.floatValue.dp.toPx() }
    val skewXPx = with(density) { WheelConfig.pivotSkewXDp.floatValue.dp.toPx() }
    val skewYPx = with(density) { WheelConfig.pivotSkewYDp.floatValue.dp.toPx() }
    val nameOnRight = if (WheelConfig.displayStyle.value == WheelDisplayStyle.CENTER) WheelConfig.arcMirrored.value else !WheelConfig.arcMirrored.value

    val visualOffset = remember(currentFolder) { Animatable(0f) }
    var previousIndex by remember(currentFolder) { mutableStateOf(wheelState.columnIndex) }

    LaunchedEffect(wheelScrollProgress, isRotatingDrag) {
        if (isRotatingDrag) visualOffset.snapTo(wheelScrollProgress)
    }
    LaunchedEffect(isRotatingDrag) {
        if (!isRotatingDrag) visualOffset.animateTo(0f, tween(150))
    }
    LaunchedEffect(wheelState.columnIndex, apps.size) {
        if (!isRotatingDrag && wheelState.columnIndex != previousIndex && apps.isNotEmpty()) {
            val rawDelta = wheelState.columnIndex - previousIndex
            val direction = when {
                rawDelta > apps.size / 2 -> -1
                rawDelta < -apps.size / 2 -> 1
                rawDelta > 0 -> 1
                rawDelta < 0 -> -1
                else -> 0
            }
            if (direction != 0) {
                visualOffset.snapTo(-direction.toFloat())
                visualOffset.animateTo(0f, tween(180))
            }
        }
        previousIndex = wheelState.columnIndex
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Folder strip renders at TOP or BOTTOM of this whole display area, per toggle.
        if (WheelConfig.folderStripPosition.value == FolderStripPosition.TOP) {
            FolderStrip(currentFolder, prevFolder, nextFolder, categories.size, onBg, accent)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val baseArcCenterYPx = containerHeightPx / 2f

            val pivotXPx = when (WheelConfig.displayStyle.value) {
                WheelDisplayStyle.CENTER -> containerWidthPx / 2f + skewXPx
                WheelDisplayStyle.EDGE -> if (!WheelConfig.arcMirrored.value) edgeMarginPx + skewXPx else containerWidthPx - edgeMarginPx - skewXPx
            }
            val pivotYPx = baseArcCenterYPx - skewYPx

            fun positionForAngle(angleFromApex: Float): Offset {
                val dx = mirror * radiusPx * cos(angleFromApex)
                val dy = radiusPx * sin(angleFromApex)
                return Offset(pivotXPx + dx, pivotYPx + dy)
            }

            if (debug) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var prev: Offset? = null
                    var a = 0f
                    while (a <= 2 * Math.PI.toFloat() + 0.05f) {
                        val point = positionForAngle(a)
                        prev?.let { drawLine(Color.Magenta.copy(alpha = 0.4f), it, point, strokeWidth = 2f) }
                        prev = point
                        a += 0.1f
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset((pivotXPx - ringRadiusPx).roundToInt(), (pivotYPx - ringRadiusPx).roundToInt()) }
                    .size(16.dp)
                    .border(width = 1.dp, color = if (debug) Color.Magenta else mutedColor.copy(alpha = 0.2f), shape = CircleShape)
            )

            run {
                val apexPoint = positionForAngle(0f)
                val ringEdgePoint = Offset(pivotXPx + mirror * ringRadiusPx, pivotYPx)
                val thickness = with(density) { WheelConfig.connectorLineThicknessDp.floatValue.dp.toPx() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (WheelConfig.connectorGradientEnabled.value) {
                        val startFrac = WheelConfig.connectorGradientStartFraction.floatValue.coerceIn(0f, 0.99f)
                        val brush = Brush.linearGradient(
                            colorStops = arrayOf(0f to onBg.copy(alpha = 0.35f), startFrac to onBg.copy(alpha = 0.35f), 1f to onBg.copy(alpha = 0f)),
                            start = ringEdgePoint, end = apexPoint
                        )
                        drawLine(brush, ringEdgePoint, apexPoint, strokeWidth = thickness)
                    } else {
                        drawLine(onBg.copy(alpha = 0.35f), ringEdgePoint, apexPoint, strokeWidth = thickness)
                    }
                }
            }

            if (apps.isNotEmpty()) {
                val selectedIndex = wheelState.columnIndex.coerceIn(0, apps.size - 1)
                val visibleRange = -3..3

                // Big prominent title — RESTORED to original styling: accent-colored
                // first letter, rest in onBg, Light weight, single-line clipped.
                val bigTitleWidthDp = 200.dp
                val bigTitleWidthPx = with(density) { bigTitleWidthDp.toPx() }
                val bigTitleLeftPx = if (nameOnRight) containerWidthPx - bigTitleWidthPx - 24f else 24f
                val bigTitleAlign = if (nameOnRight) Alignment.CenterEnd else Alignment.CenterStart
                val label = apps[selectedIndex].label.capitalizeFirstOnly()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(bigTitleLeftPx.roundToInt(), 0) }
                        .width(bigTitleWidthDp)
                        .fillMaxSize(),
                    contentAlignment = bigTitleAlign
                ) {
                    Column(horizontalAlignment = if (nameOnRight) Alignment.End else Alignment.Start) {
                        Box(modifier = Modifier.width(bigTitleWidthDp)) {
                            Text(
                                text = buildAnnotatedString {
                                    if (label.isNotEmpty()) {
                                        withStyle(SpanStyle(color = accent)) { append(label.first().toString()) }
                                        withStyle(SpanStyle(color = onBg)) { append(label.drop(1)) }
                                    }
                                },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Light,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                softWrap = false
                            )
                        }
                        Text("Installed App", color = onBg.copy(alpha = 0.4f), fontSize = 15.sp, fontWeight = FontWeight.Light)
                    }
                }

                for (offset in visibleRange) {
                    val idx = ((selectedIndex + offset) % apps.size + apps.size) % apps.size
                    val angle = (offset - visualOffset.value) * angularGapRad
                    val point = positionForAngle(angle)
                    val normDist = Math.abs(offset - visualOffset.value) / 1f

                    val sizeDp = lerp3(normDist, fullIconSizeDp, fullIconSizeDp * 0.7f, fullIconSizeDp * 0.5f)
                    val alpha = lerp3(normDist, 1f, 0.45f, 0.15f)
                    val halfSizePx = with(density) { sizeDp.dp.toPx() } / 2f

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((point.x - halfSizePx).roundToInt(), (point.y - halfSizePx).roundToInt()) }
                            .then(if (debug) Modifier.border(1.dp, Color.Green.copy(alpha = 0.6f)) else Modifier)
                    ) {
                        Image(bitmap = apps[idx].icon, contentDescription = apps[idx].label, alpha = alpha, modifier = Modifier.size(sizeDp.dp))
                    }

                    if (offset != 0) {
                        val labelWidthDp = 90.dp
                        val labelWidthPx = with(density) { labelWidthDp.toPx() }
                        val gapPx = with(density) { 8.dp.toPx() }
                        val labelLeftPx = if (mirror > 0) point.x - halfSizePx - gapPx - labelWidthPx else point.x + halfSizePx + gapPx
                        val labelAlign = if (mirror > 0) Alignment.CenterEnd else Alignment.CenterStart
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(labelLeftPx.roundToInt(), (point.y - with(density) { 20.dp.toPx() }).roundToInt()) }
                                .width(labelWidthDp)
                                .height(40.dp),
                            contentAlignment = labelAlign
                        ) {
                            Column(horizontalAlignment = if (mirror > 0) Alignment.End else Alignment.Start) {
                                Text(apps[idx].label.capitalizeFirstOnly(), color = onBg.copy(alpha = alpha), fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                Text("Installed App", color = onBg.copy(alpha = alpha * 0.6f), fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                Text("No Apps In This Folder", color = mutedColor, fontSize = 16.sp, modifier = Modifier.offset { IntOffset(24, (containerHeightPx / 2f).roundToInt()) })
            }
        }

        if (WheelConfig.folderStripPosition.value == FolderStripPosition.BOTTOM) {
            FolderStrip(currentFolder, prevFolder, nextFolder, categories.size, onBg, accent)
        }
    }
}