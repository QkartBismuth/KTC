package com.qkartbismuth.ktc.utils

import android.content.Context
import com.qkartbismuth.ktc.Preferences
import com.qkartbismuth.ktc.R

/**
 * @param context application context
 */
class AppDynamicTheme(
    private val context: Context
) {
    companion object {
        const val DEFAULT_THEME = R.style.Theme_KTC
        const val DARK_THEME = R.style.Theme_KTC
        const val OLED_THEME = R.style.Theme_KTC_Oled
    }

    /**
     * Loads theme from preferences.
     * Dark mode reuses the base M3 theme but forces night mode
     * (handled by MainActivity), while OLED always uses the pure
     * black palette.
     */
    fun loadTheme() {
        when (Preferences.currentTheme) {
            "oled" -> context.setTheme(OLED_THEME)
            else -> context.setTheme(DEFAULT_THEME)
        }
    }
}