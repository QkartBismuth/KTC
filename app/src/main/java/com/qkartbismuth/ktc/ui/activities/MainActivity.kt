package com.qkartbismuth.ktc.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.qkartbismuth.ktc.Preferences
import com.qkartbismuth.ktc.databinding.ActivityMainBinding
import com.qkartbismuth.ktc.utils.AppDynamicTheme

/**
 * The main app activity.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load the selected theme and apply it BEFORE the activity is
        // created so DayNight resolution sees the right mode.
        Preferences(this).load()
        applyNightMode()
        AppDynamicTheme(this).loadTheme()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Forces night mode for the dark and OLED themes so the
     * value-night resources and the OLED palette are applied.
     */
    private fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(
            when (Preferences.currentTheme) {
                "dark", "oled" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}