package com.example.rotarylauncher

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Folder(
    val id: String,
    var name: String,
    val appPackageNames: SnapshotStateList<String> = mutableStateListOf()
)

object FoldersConfig {
    val folders = mutableStateOf<List<Folder>>(emptyList())
}