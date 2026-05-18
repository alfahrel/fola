package alfahrel.my.id.fola

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePrefs {

    private const val PREFS_NAME = "fola_theme_prefs"
    private const val KEY_MODE = "night_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    fun getMode(ctx: Context): Int {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setMode(ctx: Context, mode: Int) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MODE, mode).apply()
    }

    fun isDynamicColor(ctx: Context): Boolean {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_COLOR, true)
    }

    fun setDynamicColor(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }
}