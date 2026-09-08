package com.example.rotarylauncher

import androidx.compose.runtime.mutableStateOf

object IconPackConfig {
    val selectedPackPackage = mutableStateOf<String?>(null) // null = use each app's own icon
}