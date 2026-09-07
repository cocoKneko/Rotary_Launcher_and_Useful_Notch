package com.example.usefulnotch

import android.content.Context

object NotchConfigPersistence {
    private const val PREFS_NAME = "notch_config"

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        NotchConfig.widthPx.value = p.getFloat("widthPx", NotchConfig.widthPx.value)
        NotchConfig.heightPx.value = p.getFloat("heightPx", NotchConfig.heightPx.value)
        NotchConfig.cornerRadiusPx.value = p.getFloat("cornerRadiusPx", NotchConfig.cornerRadiusPx.value)
        NotchConfig.pillXPx.value = p.getFloat("pillXPx", NotchConfig.pillXPx.value)
        NotchConfig.pillYPx.value = p.getFloat("pillYPx", NotchConfig.pillYPx.value)
        NotchConfig.shape.value = NotchShape.values()[p.getInt("shape", NotchConfig.shape.value.ordinal)]
        NotchConfig.horizontalAnchor.value = HorizontalAnchor.values()[p.getInt("horizontalAnchor", NotchConfig.horizontalAnchor.value.ordinal)]
        NotchConfig.screenTarget.value = ScreenTarget.values()[p.getInt("screenTarget", NotchConfig.screenTarget.value.ordinal)]
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat("widthPx", NotchConfig.widthPx.value)
            .putFloat("heightPx", NotchConfig.heightPx.value)
            .putFloat("cornerRadiusPx", NotchConfig.cornerRadiusPx.value)
            .putFloat("pillXPx", NotchConfig.pillXPx.value)
            .putFloat("pillYPx", NotchConfig.pillYPx.value)
            .putInt("shape", NotchConfig.shape.value.ordinal)
            .putInt("horizontalAnchor", NotchConfig.horizontalAnchor.value.ordinal)
            .putInt("screenTarget", NotchConfig.screenTarget.value.ordinal)
            .apply()
    }
}