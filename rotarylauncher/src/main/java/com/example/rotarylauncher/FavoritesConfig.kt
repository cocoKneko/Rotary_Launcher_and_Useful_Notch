package com.example.rotarylauncher

import androidx.compose.runtime.mutableStateOf

object FavoritesConfig {
    val slot1 = mutableStateOf<String?>(null)
    val slot2 = mutableStateOf<String?>(null)
    val slot3 = mutableStateOf<String?>(null)
    val slot4 = mutableStateOf<String?>(null)

    fun slots() = listOf(slot1, slot2, slot3, slot4)

    fun assignFirstEmpty(packageName: String): Boolean {
        val empty = slots().firstOrNull { it.value == null } ?: return false
        empty.value = packageName
        return true
    }
}