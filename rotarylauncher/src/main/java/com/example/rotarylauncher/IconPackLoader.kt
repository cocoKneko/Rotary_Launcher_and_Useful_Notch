package com.example.rotarylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(val packageName: String, val label: String)

// Every icon-pack "standard" third-party launchers agreed on informally — checking
// all of them maximizes compatibility with packs users already own.
private val ICON_PACK_ACTIONS = listOf(
    "com.novalauncher.THEME",
    "org.adw.launcher.THEMES",
    "com.gau.go.launcherex.theme",
    "com.anddoes.launcher.THEME"
)

fun findInstalledIconPacks(pm: PackageManager): List<IconPackInfo> {
    val found = linkedMapOf<String, IconPackInfo>()
    ICON_PACK_ACTIONS.forEach { action ->
        val intent = Intent(action)
        pm.queryIntentActivities(intent, 0).forEach { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg !in found) {
                found[pkg] = IconPackInfo(pkg, resolveInfo.loadLabel(pm).toString())
            }
        }
    }
    return found.values.toList()
}

// Component name format matches what appfilter.xml files actually contain:
// ComponentInfo{packagename/packagename.ActivityName}
private fun componentKey(packageName: String, activityClass: String) = "ComponentInfo{$packageName/$activityClass}"

fun loadIconPackMapping(pm: PackageManager, iconPackPackage: String): Map<String, String> {
    val mapping = mutableMapOf<String, String>()
    try {
        val res = pm.getResourcesForApplication(iconPackPackage)
        val xmlId = res.getIdentifier("appfilter", "xml", iconPackPackage)
        if (xmlId == 0) return emptyMap()
        val parser = res.getXml(xmlId)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) mapping[component] = drawable
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        return emptyMap()
    }
    return mapping
}

fun resolvePackedIcon(pm: PackageManager, iconPackPackage: String, mapping: Map<String, String>, appPackageName: String, activityClassName: String): Drawable? {
    val key = componentKey(appPackageName, activityClassName)
    val drawableName = mapping[key] ?: return null
    return try {
        val res = pm.getResourcesForApplication(iconPackPackage)
        val resId = res.getIdentifier(drawableName, "drawable", iconPackPackage)
        if (resId == 0) null else res.getDrawable(resId, null)
    } catch (e: Exception) {
        null
    }
}