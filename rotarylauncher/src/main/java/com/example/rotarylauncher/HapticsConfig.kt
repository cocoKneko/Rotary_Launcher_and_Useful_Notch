package com.example.rotarylauncher

import androidx.compose.runtime.mutableFloatStateOf

object HapticsConfig {
    val buttonIntensityMs = mutableFloatStateOf(30f)
    val detentIntensityMs = mutableFloatStateOf(15f)
    val buttonDelayMs = mutableFloatStateOf(0f)
    val detentDelayMs = mutableFloatStateOf(0f)
}