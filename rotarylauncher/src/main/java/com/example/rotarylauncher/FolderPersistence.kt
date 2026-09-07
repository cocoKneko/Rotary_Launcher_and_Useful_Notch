package com.example.rotarylauncher

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

object FolderPersistence {
    private const val PREFS_NAME = "folder_config"
    private const val KEY_FOLDERS = "folders_json"

    fun load(context: Context, allDetectedApps: List<AppEntry>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FOLDERS, null)

        if (json == null) {
            FoldersConfig.folders.value = emptyList()
        } else {
            val array = JSONArray(json)
            val loaded = mutableListOf<Folder>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkgArray = obj.getJSONArray("apps")
                val pkgs = mutableStateListOf<String>()
                for (j in 0 until pkgArray.length()) pkgs.add(pkgArray.getString(j))
                loaded.add(Folder(id = obj.getString("id"), name = obj.getString("name"), appPackageNames = pkgs))
            }
            FoldersConfig.folders.value = loaded
        }

        FavoritesConfig.slot1.value = prefs.getString("fav1", null)
        FavoritesConfig.slot2.value = prefs.getString("fav2", null)
        FavoritesConfig.slot3.value = prefs.getString("fav3", null)
        FavoritesConfig.slot4.value = prefs.getString("fav4", null)

        save(context)
    }

    fun save(context: Context) {
        val array = JSONArray()
        FoldersConfig.folders.value.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            obj.put("apps", JSONArray(folder.appPackageNames.toList()))
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_FOLDERS, array.toString())
            .putString("fav1", FavoritesConfig.slot1.value)
            .putString("fav2", FavoritesConfig.slot2.value)
            .putString("fav3", FavoritesConfig.slot3.value)
            .putString("fav4", FavoritesConfig.slot4.value)
            .apply()
    }
}