package com.example.rotarylauncher

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun fireHaptic(context: Context, scope: CoroutineScope, durationMs: Float, delayMs: Float) {
    scope.launch {
        if (delayMs > 0f) delay(delayMs.toLong())
        val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                durationMs.toLong().coerceAtLeast(1L),
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }
}