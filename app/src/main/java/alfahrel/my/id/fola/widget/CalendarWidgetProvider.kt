package alfahrel.my.id.fola.widget

import alfahrel.my.id.fola.ui.calendar.CalendarActivity
import alfahrel.my.id.fola.R
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import java.util.Calendar

class CalendarWidgetProvider : AppWidgetProvider() {

    companion object {
        const val TAG = "CalendarWidgetProvider"
        const val ACTION_PREV = "alfahrel.my.id.fola.CALENDAR_WIDGET_PREV"
        const val ACTION_NEXT = "alfahrel.my.id.fola.CALENDAR_WIDGET_NEXT"
        const val ACTION_MIDNIGHT_UPDATE = "alfahrel.my.id.fola.CALENDAR_WIDGET_MIDNIGHT_UPDATE"
        const val EXTRA_WIDGET = "widget_id"
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 998

        fun scheduleMidnightAlarm(ctx: Context) {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }
            val intent = Intent(ctx, CalendarWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            val pi = PendingIntent.getBroadcast(
                ctx,
                MIDNIGHT_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, midnight.timeInMillis, pi)
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC, midnight.timeInMillis, pi)
                Log.w(TAG, "Exact alarm not permitted, falling back: ${e.message}")
            }
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(ctx, mgr, it) }
        scheduleMidnightAlarm(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_MIDNIGHT_UPDATE -> {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(
                    ComponentName(
                        ctx,
                        CalendarWidgetProvider::class.java
                    )
                )
                ids.forEach { updateWidget(ctx, mgr, it) }
                scheduleMidnightAlarm(ctx)
                return
            }
        }

        val widgetId = intent.getIntExtra(EXTRA_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val current   = getOffset(ctx, widgetId)
        val newOffset = when (intent.action) {
            ACTION_PREV -> current - 1
            ACTION_NEXT -> current + 1
            else        -> current
        }
        setOffset(ctx, widgetId, newOffset)
        updateWidget(ctx, AppWidgetManager.getInstance(ctx), widgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs  = context.getSharedPreferences("calendar_widget_fola", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        appWidgetIds.forEach { editor.remove("offset_$it") }
        editor.apply()
    }

    private fun updateWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
        val offset = getOffset(ctx, widgetId)
        val cal    = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        val year   = cal.get(Calendar.YEAR)
        val month  = cal.get(Calendar.MONTH)
        val today  = Calendar.getInstance()
        val stamp  = System.currentTimeMillis()

        val monthNames    = ctx.resources.getStringArray(R.array.month_names)
        val monthYearText = "${monthNames[month]} $year"

        val views = RemoteViews(ctx.packageName, R.layout.widget_calendar)

        views.setTextViewText(R.id.widget_tv_month_year, monthYearText)
        views.setOnClickPendingIntent(R.id.widget_btn_prev, buildIntent(ctx, widgetId, ACTION_PREV))
        views.setOnClickPendingIntent(R.id.widget_btn_next, buildIntent(ctx, widgetId, ACTION_NEXT))

        val openApp = PendingIntent.getActivity(
            ctx,
            widgetId,
            Intent(ctx, CalendarActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_tv_month_year, openApp)

        val serviceIntent = Intent(ctx, CalendarWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra("year", year)
            putExtra("month", month)
            data = Uri.parse("widget://$widgetId/$year/$month/$stamp")
        }
        views.setRemoteAdapter(R.id.widget_grid_calendar, serviceIntent)
        views.setPendingIntentTemplate(R.id.widget_grid_calendar, openApp)

        mgr.updateAppWidget(widgetId, views)
        mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_grid_calendar)
    }

    private fun buildIntent(ctx: Context, widgetId: Int, action: String): PendingIntent {
        val intent = Intent(ctx, CalendarWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_WIDGET, widgetId)
            data = Uri.parse("widget://$widgetId/$action")
        }
        val requestCode = when (action) {
            ACTION_PREV -> widgetId * 10
            ACTION_NEXT -> widgetId * 10 + 1
            else        -> widgetId
        }
        return PendingIntent.getBroadcast(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOffset(ctx: Context, widgetId: Int): Int =
        ctx.getSharedPreferences("calendar_widget_fola", Context.MODE_PRIVATE)
            .getInt("offset_$widgetId", 0)

    private fun setOffset(ctx: Context, widgetId: Int, value: Int) {
        ctx.getSharedPreferences("calendar_widget_fola", Context.MODE_PRIVATE)
            .edit().putInt("offset_$widgetId", value).apply()
    }
}