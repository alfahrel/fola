package alfahrel.my.id.fola.util

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.ThemePrefs
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import java.util.Locale

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.getMode(this))
        applyDynamicColors()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(applyLocale(base))
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

    companion object {
        fun applyLocale(context: Context): Context {
            val langCode = LanguagePrefs.loadLanguage(context)
            val locale   = Locale(langCode)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
}