package com.example.rotarylauncher

import androidx.compose.runtime.mutableStateOf

enum class SettingsTab { TUNING, APPS }

object AppSettingsState {
    val darkModeOverride = mutableStateOf<Boolean?>(null)
    val showSettings = mutableStateOf(false)
    val settingsTab = mutableStateOf(SettingsTab.TUNING)
}