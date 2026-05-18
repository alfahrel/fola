package alfahrel.my.id.fola.util

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.ThemePrefs
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.getMode(this))
        applyDynamicColors()
    }

    fun applyDynamicColors() {
        val options = if (ThemePrefs.isDynamicColor(this)) {
            DynamicColorsOptions.Builder().build()
        } else {
            DynamicColorsOptions.Builder()
                .setThemeOverlay(R.style.ThemeOverlay_Fola_Default)
                .build()
        }
        DynamicColors.applyToActivitiesIfAvailable(this, options)
    }
}