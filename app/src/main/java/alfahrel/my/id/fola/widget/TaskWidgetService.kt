package alfahrel.my.id.fola.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.data.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskRemoteViewsFactory(this, intent)
}

class TaskRemoteViewsFactory(
    private val service: RemoteViewsService,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var activeTasks:    List<Task> = emptyList()
    private var completedTasks: List<Task> = emptyList()
    private var showCompleted:  Boolean    = false
    private var expanded:       Boolean    = true
    private var showCategory:   Boolean    = true
    private var showDate:       Boolean    = true
    private val fmt = SimpleDateFormat("MMM d", Locale.getDefault())

    companion object {
        private const val TYPE_TASK   = 0
        private const val TYPE_HEADER = 1
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val dateFilter   = WidgetPrefs.loadDateFilter(service, appWidgetId)
        val showDone     = WidgetPrefs.loadShowCompleted(service, appWidgetId)
        val expandedPref = WidgetPrefs.loadCompletedExpanded(service, appWidgetId)
        val db           = AppDatabase.getInstance(service)
        val latch        = CountDownLatch(1)
        var active       = emptyList<Task>()
        var completed    = emptyList<Task>()

        Thread {
            if (dateFilter == null) {
                active    = db.taskDao().getAllActiveTasksSync()
                completed = if (showDone) db.taskDao().getAllCompletedTasksSync() else emptyList()
            } else {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
                }
                val startOfDay  = cal.timeInMillis
                val windowStart = if (dateFilter == 1) startOfDay + 86_400_000L else startOfDay
                val windowEnd   = startOfDay + (dateFilter + 1) * 86_400_000L
                active    = db.taskDao().getActiveTasksForDaySync(windowStart, windowEnd)
                completed = if (showDone) db.taskDao().getCompletedTasksForDaySync(windowStart, windowEnd) else emptyList()
            }
            latch.countDown()
        }.start()

        latch.await()
        activeTasks    = active
        completedTasks = completed
        showCompleted  = showDone
        expanded       = expandedPref
        showCategory   = WidgetPrefs.loadShowCategory(service, appWidgetId)
        showDate       = WidgetPrefs.loadShowDate(service, appWidgetId)
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        var count = activeTasks.size
        if (showCompleted && completedTasks.isNotEmpty()) {
            count += 1
            if (expanded) count += completedTasks.size
        }
        return count
    }

    override fun getViewTypeCount() = 2

    fun getItemViewType(position: Int): Int {
        if (position < activeTasks.size) return TYPE_TASK
        return if (position == activeTasks.size) TYPE_HEADER else TYPE_TASK
    }

    override fun getItemId(position: Int): Long {
        if (position < activeTasks.size) return activeTasks[position].id
        if (position == activeTasks.size) return -1L
        return completedTasks[position - activeTasks.size - 1].id
    }

    override fun hasStableIds() = true

    override fun getViewAt(position: Int): RemoteViews {
        if (showCompleted && completedTasks.isNotEmpty() && position == activeTasks.size) {
            return buildHeaderView()
        }
        val task = if (position < activeTasks.size) {
            activeTasks[position]
        } else {
            completedTasks[position - activeTasks.size - 1]
        }
        return buildTaskView(task)
    }

    private fun buildHeaderView(): RemoteViews {
        val views = RemoteViews(service.packageName, R.layout.widget_task_header)
        views.setTextViewText(R.id.widgetHeaderText, "Completed (${completedTasks.size})")
        views.setImageViewResource(
            R.id.widgetHeaderChevron,
            if (expanded) R.drawable.ic_rounded_expand_circle_down_24
            else          R.drawable.ic_rounded_expand_circle_up_24
        )
        val fillIntent = Intent().apply {
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID, -1L)
            putExtra("is_header", true)
        }
        views.setOnClickFillInIntent(R.id.widgetHeaderText,    fillIntent)
        views.setOnClickFillInIntent(R.id.widgetHeaderChevron, fillIntent)
        return views
    }

    private fun buildTaskView(task: Task): RemoteViews {
        val views = RemoteViews(service.packageName, R.layout.widget_task_item)

        val now       = System.currentTimeMillis()
        val isOverdue = !task.isCompleted && task.dueDateMs != null &&
                (task.dueDateMs + 86_400_000L - 1) < now

        val cardBg = if (isOverdue)
            service.getColor(R.color.widget_overdue_bg)
        else
            service.getColor(R.color.widget_card_normal_bg)
        views.setInt(R.id.widgetItemRoot, "setBackgroundColor", cardBg)

        views.setTextViewText(R.id.widgetItemTitle, task.title)

        val titleColor = if (isOverdue)
            service.getColor(R.color.widget_overdue_text)
        else
            service.getColor(R.color.widget_text_primary)
        views.setTextColor(R.id.widgetItemTitle, titleColor)

        if (showCategory) {
            views.setViewVisibility(R.id.widgetItemCategory, View.VISIBLE)
            views.setTextViewText(R.id.widgetItemCategory, task.category)
        } else {
            views.setViewVisibility(R.id.widgetItemCategory, View.GONE)
        }

        if (showDate) {
            when {
                task.dueDateMs != null -> {
                    views.setViewVisibility(R.id.widgetItemDue, View.VISIBLE)
                    val label = if (isOverdue)
                        "Overdue ${fmt.format(Date(task.dueDateMs))}"
                    else
                        "Due ${fmt.format(Date(task.dueDateMs))}"
                    views.setTextViewText(R.id.widgetItemDue, label)
                    val dueColor = if (isOverdue)
                        service.getColor(R.color.widget_overdue_text)
                    else
                        service.getColor(R.color.widget_text_secondary)
                    views.setTextColor(R.id.widgetItemDue, dueColor)
                }
                task.startDateMs != null -> {
                    views.setViewVisibility(R.id.widgetItemDue, View.VISIBLE)
                    views.setTextViewText(R.id.widgetItemDue, "Starts ${fmt.format(Date(task.startDateMs))}")
                    views.setTextColor(R.id.widgetItemDue, service.getColor(R.color.widget_text_secondary))
                }
                else -> views.setViewVisibility(R.id.widgetItemDue, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widgetItemDue, View.GONE)
        }

        if (task.isCompleted) {
            views.setInt(R.id.widgetItemTitle, "setPaintFlags",
                Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            views.setFloat(R.id.widgetItemTitle, "setAlpha", 0.4f)
            views.setImageViewResource(R.id.widgetItemCheck, R.drawable.ic_widget_checkbox_checked)
            views.setInt(R.id.widgetItemCheck, "setColorFilter", service.getColor(R.color.widget_text_secondary))
        } else {
            views.setInt(R.id.widgetItemTitle, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            views.setFloat(R.id.widgetItemTitle, "setAlpha", 1f)
            views.setImageViewResource(R.id.widgetItemCheck, R.drawable.ic_widget_checkbox_unchecked)
            views.setInt(R.id.widgetItemCheck, "setColorFilter", service.getColor(R.color.widget_text_secondary))
        }

        val fillIntent = Intent().apply {
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID,   task.id)
            putExtra(TaskWidgetProvider.EXTRA_COMPLETED, task.isCompleted)
            putExtra("is_header", false)
        }
        views.setOnClickFillInIntent(R.id.widgetItemCheck, fillIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
}