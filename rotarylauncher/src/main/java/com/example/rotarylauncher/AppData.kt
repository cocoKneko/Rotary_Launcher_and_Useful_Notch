package com.example.rotarylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val label: String,
    val packageName: String,
    val launchIntent: Intent,
    val icon: ImageBitmap
)
data class Category(val name: String, val apps: List<AppEntry>)

private fun safeIconBitmap(drawable: Drawable): ImageBitmap {
    return try {
        drawable.toBitmap(width = 108, height = 108, config = Bitmap.Config.ARGB_8888).asImageBitmap()
    } catch (e: Exception) {
        // Some adaptive/vector icons can fail to rasterize with certain sizes —
        // fall back to a blank bitmap rather than crashing the whole app list.
        Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

fun detectInstalledApps(pm: PackageManager): List<AppEntry> {
    val mainIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(mainIntent, 0)
        .map { resolveInfo ->
            AppEntry(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                launchIntent = pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                    ?: Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name),
                icon = safeIconBitmap(resolveInfo.loadIcon(pm))
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

fun buildDefaultCategories(pm: PackageManager): List<Category> {
    return listOf(Category("All Apps", detectInstalledApps(pm)))
}