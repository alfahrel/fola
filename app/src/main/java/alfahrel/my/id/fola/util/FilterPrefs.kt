package alfahrel.my.id.fola.util

import android.content.Context

object FilterPrefs {

    private const val PREFS_NAME      = "fola_filter_prefs"
    private const val KEY_DATE_FILTER = "date_filter"
    private const val KEY_CAT_FILTER  = "category_filter"

    fun saveDateFilter(ctx: Context, daysAhead: Int?) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DATE_FILTER, daysAhead ?: -1).apply()
    }

    fun loadDateFilter(ctx: Context): Int? {
        val v = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DATE_FILTER, -1)
        return if (v == -1) null else v
    }

    fun saveCategoryFilter(ctx: Context, category: String?) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CAT_FILTER, category ?: "").apply()
    }

    fun loadCategoryFilter(ctx: Context): String? {
        val v = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CAT_FILTER, "") ?: ""
        return if (v.isEmpty()) null else v
    }
}