package com.example.usefulnotch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotchOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var screenWidthPx: Int = 0

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()
    )

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForegroundWithNotification()
        addOverlayView()
        observeSizeChanges()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = overlayView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        applyAnchorAndPosition(params, params.width)
        windowManager.updateViewLayout(view, params)
    }

    private fun startForegroundWithNotification() {
        val channelId = "notch_overlay_channel"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notch Overlay", NotificationManager.IMPORTANCE_MIN)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Notch overlay active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
    }

    private fun refreshScreenWidth() {
        screenWidthPx = windowManager.currentWindowMetrics.bounds.width()
        NotchConfig.screenWidthPx.value = screenWidthPx.toFloat()
    }

    private fun addOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        refreshScreenWidth()

        val initialWidth = NotchConfig.widthPx.value.toInt()
        val initialHeight = NotchConfig.heightPx.value.toInt()

        val params = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        applyAnchorAndPosition(params, initialWidth)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@NotchOverlayService)
            setViewTreeSavedStateRegistryOwner(this@NotchOverlayService)
            setViewTreeViewModelStoreOwner(this@NotchOverlayService)
            setContent { NotchOverlayContent() }
        }

        overlayView = view
        windowManager.addView(view, params)
    }

    private fun applyAnchorAndPosition(params: WindowManager.LayoutParams, width: Int) {
        params.gravity = Gravity.TOP or Gravity.START
        refreshScreenWidth()

        if (NotchConfig.shape.value == NotchShape.WATERFALL) {
            params.y = 0
            params.x = NotchConfig.waterfallLeftEdge(
                NotchConfig.horizontalAnchor.value,
                NotchConfig.screenTarget.value,
                screenWidthPx.toFloat(),
                width.toFloat()
            ).toInt()
        } else {
            params.x = NotchConfig.pillLeftEdge(
                NotchConfig.pillXPx.value,
                NotchConfig.horizontalAnchor.value,
                width.toFloat()
            ).toInt()
            params.y = NotchConfig.pillYPx.value.toInt()
        }
    }

    private fun observeSizeChanges() {
        serviceScope.launch {
            androidx.compose.runtime.snapshotFlow {
                listOf(
                    NotchConfig.widthPx.value,
                    NotchConfig.heightPx.value,
                    NotchConfig.horizontalAnchor.value.ordinal.toFloat(),
                    NotchConfig.pillXPx.value,
                    NotchConfig.pillYPx.value,
                    NotchConfig.shape.value.ordinal.toFloat(),
                    NotchConfig.screenTarget.value.ordinal.toFloat()
                )
            }.collect { values ->
                val (w, h) = values[0] to values[1]
                val view = overlayView ?: return@collect
                val params = view.layoutParams as? WindowManager.LayoutParams ?: return@collect
                params.width = w.toInt()
                params.height = h.toInt()
                applyAnchorAndPosition(params, w.toInt())
                windowManager.updateViewLayout(view, params)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun NotchOverlayContent() {
    var pressColor by remember { mutableStateOf(Color.Black) }
    val scope = rememberCoroutineScope()

    fun flash(color: Color) {
        pressColor = color
        scope.launch {
            delay(1500)
            pressColor = Color.Black
        }
    }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { flash(Color.Red) },
                    onDoubleTap = { flash(Color.Blue) },
                    onLongPress = { flash(Color.Green) }
                )
            }
    ) {
        PlaceholderPill(fillColor = pressColor)
    }
}

@Composable
fun PlaceholderPill(fillColor: Color = Color.Black) {
    val width = NotchConfig.widthPx.value
    val height = NotchConfig.heightPx.value
    val radius = NotchConfig.cornerRadiusPx.value
    val shape = NotchConfig.shape.value
    val safeRadius = if (shape == NotchShape.WATERFALL) {
        minOf(radius, width / 4f, height / 2f)
    } else {
        minOf(radius, width / 2f, height / 2f)
    }

    val density = LocalDensity.current
    val widthDp = with(density) { width.toDp() }
    val heightDp = with(density) { height.toDp() }

    if (shape == NotchShape.PILL) {
        val radiusDp = with(density) { safeRadius.toDp() }
        Box(
            modifier = Modifier
                .size(width = widthDp, height = heightDp)
                .clip(RoundedCornerShape(radiusDp))
                .background(fillColor)
        )
    } else {
        val anchor = NotchConfig.horizontalAnchor.value
        Canvas(modifier = Modifier.size(width = widthDp, height = heightDp)) {
            val w = size.width
            val h = size.height
            val r = safeRadius
            val leftFlush = anchor == HorizontalAnchor.LEFT
            val rightFlush = anchor == HorizontalAnchor.RIGHT

            val path = Path().apply {
                moveTo(0f, 0f)
                if (leftFlush) {
                    lineTo(0f, h)
                } else {
                    quadraticBezierTo(r, 0f, r, r)
                    lineTo(r, h - r)
                    quadraticBezierTo(r, h, 2 * r, h)
                }
                val rightBottomX = if (rightFlush) w else w - 2 * r
                lineTo(rightBottomX, h)
                if (rightFlush) {
                    lineTo(w, h)
                    lineTo(w, 0f)
                } else {
                    quadraticBezierTo(w - r, h, w - r, h - r)
                    lineTo(w - r, r)
                    quadraticBezierTo(w - r, 0f, w, 0f)
                }
                close()
            }
            drawPath(path, color = fillColor)
        }
    }
}