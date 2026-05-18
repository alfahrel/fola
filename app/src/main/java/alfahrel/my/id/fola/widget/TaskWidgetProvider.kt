package alfahrel.my.id.fola.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import alfahrel.my.id.fola.ui.home.MainActivity
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.data.model.Task
import java.util.Calendar

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )

        when (intent.action) {
            ACTION_TOGGLE_TASK -> {
                val isHeader  = intent.getBooleanExtra("is_header", false)
                val taskId    = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val completed = intent.getBooleanExtra(EXTRA_COMPLETED, false)

                if (isHeader) {
                    val current = WidgetPrefs.loadCompletedExpanded(context, appWidgetId)
                    WidgetPrefs.saveCompletedExpanded(context, appWidgetId, !current)
                    updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }

                if (taskId == -1L) return
                Thread {
                    AppDatabase.getInstance(context).taskDao().setCompletedSync(taskId, !completed)
                    Handler(Looper.getMainLooper()).post {
                        updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    }
                }.start()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.clear(context, it) }
    }

    companion object {

        const val ACTION_TOGGLE_TASK = "alfahrel.my.id.fola.TOGGLE_TASK"
        const val EXTRA_TASK_ID      = "extra_task_id"
        const val EXTRA_COMPLETED    = "extra_completed"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views      = RemoteViews(context.packageName, R.layout.widget_task)
            val dateFilter = WidgetPrefs.loadDateFilter(context, appWidgetId)
            val showDone   = WidgetPrefs.loadShowCompleted(context, appWidgetId)

            views.setTextViewText(R.id.widgetTitle, when (dateFilter) {
                0    -> "Today"
                1    -> "Tomorrow"
                7    -> "This Week"
                else -> "All Tasks"
            })

            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_ADD_SHEET, true)
            }
            views.setOnClickPendingIntent(
                R.id.widgetBtnAdd,
                PendingIntent.getActivity(
                    context, appWidgetId, addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(
                    "fola://widget/$appWidgetId?filter=${dateFilter}&done=${showDone}&t=${System.currentTimeMillis()}"
                )
            }
            views.setRemoteAdapter(R.id.widgetList, serviceIntent)

            val toggleTemplate = Intent(context, TaskWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TASK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setPendingIntentTemplate(
                R.id.widgetList,
                PendingIntent.getBroadcast(
                    context, appWidgetId, toggleTemplate,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )

            Thread {
                val tasks   = loadTasks(context, dateFilter, showDone)
                val total   = tasks.size
                val done    = tasks.count { it.isCompleted }
                val isEmpty = total == 0

                Handler(Looper.getMainLooper()).post {
                    views.setViewVisibility(R.id.widgetList,  if (isEmpty) View.GONE    else View.VISIBLE)
                    views.setViewVisibility(R.id.widgetEmpty, if (isEmpty) View.VISIBLE else View.GONE)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetList)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }.start()
        }

        private fun loadTasks(context: Context, dateFilter: Int?, showCompleted: Boolean): List<Task> {
            val dao = AppDatabase.getInstance(context).taskDao()
            return if (dateFilter == null) {
                if (showCompleted) dao.getAllTasksSync() else dao.getAllActiveTasksSync()
            } else {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
                }
                val startOfDay  = cal.timeInMillis
                val windowStart = if (dateFilter == 1) startOfDay + 86_400_000L else startOfDay
                val windowEnd   = startOfDay + (dateFilter + 1) * 86_400_000L
                if (showCompleted) dao.getTasksForDaySync(windowStart, windowEnd)
                else dao.getActiveTasksForDaySync(windowStart, windowEnd)
            }
        }
    }
}