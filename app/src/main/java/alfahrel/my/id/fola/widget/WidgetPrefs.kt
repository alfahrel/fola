package alfahrel.my.id.fola.widget

import android.content.Context

object WidgetPrefs {

    private const val PREFS_NAME          = "fola_widget_prefs"
    private const val KEY_DATE_FILTER     = "date_filter_"
    private const val KEY_SHOW_DONE       = "show_completed_"
    private const val KEY_DONE_EXPANDED   = "completed_expanded_"
    private const val KEY_SHOW_CATEGORY   = "show_category_"
    private const val KEY_SHOW_DATE       = "show_date_"

    fun saveDateFilter(context: Context, appWidgetId: Int, daysAhead: Int?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_DATE_FILTER + appWidgetId, daysAhead ?: -1)
            .apply()
    }

    fun loadDateFilter(context: Context, appWidgetId: Int): Int? {
        val v = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DATE_FILTER + appWidgetId, -1)
        return if (v == -1) null else v
    }

    fun saveShowCompleted(context: Context, appWidgetId: Int, show: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SHOW_DONE + appWidgetId, show)
            .apply()
    }

    fun loadShowCompleted(context: Context, appWidgetId: Int): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_DONE + appWidgetId, false)

    fun saveCompletedExpanded(context: Context, appWidgetId: Int, expanded: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DONE_EXPANDED + appWidgetId, expanded)
            .apply()
    }

    fun loadCompletedExpanded(context: Context, appWidgetId: Int): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DONE_EXPANDED + appWidgetId, true)

    fun saveShowCategory(context: Context, appWidgetId: Int, show: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SHOW_CATEGORY + appWidgetId, show)
            .apply()
    }

    fun loadShowCategory(context: Context, appWidgetId: Int): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_CATEGORY + appWidgetId, true)

    fun saveShowDate(context: Context, appWidgetId: Int, show: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SHOW_DATE + appWidgetId, show)
            .apply()
    }

    fun loadShowDate(context: Context, appWidgetId: Int): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_DATE + appWidgetId, true)

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_DATE_FILTER   + appWidgetId)
            .remove(KEY_SHOW_DONE     + appWidgetId)
            .remove(KEY_DONE_EXPANDED + appWidgetId)
            .remove(KEY_SHOW_CATEGORY + appWidgetId)
            .remove(KEY_SHOW_DATE     + appWidgetId)
            .apply()
    }
}