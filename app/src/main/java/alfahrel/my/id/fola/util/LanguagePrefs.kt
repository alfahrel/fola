package alfahrel.my.id.fola.util

import android.content.Context

object LanguagePrefs {

    private const val PREFS_NAME   = "fola_language_prefs"
    private const val KEY_LANGUAGE = "language"

    fun saveLanguage(ctx: Context, langCode: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, langCode).apply()
    }

    fun loadLanguage(ctx: Context): String {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"
    }
}