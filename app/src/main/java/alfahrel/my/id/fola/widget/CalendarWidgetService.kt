package alfahrel.my.id.fola.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.ui.calendar.HolidayType
import alfahrel.my.id.fola.ui.calendar.HolidaysData
import java.util.Calendar

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        CalendarWidgetFactory(applicationContext, intent)
}

class CalendarWidgetFactory(
    private val ctx: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    data class CellData(
        val day: Int,
        val isToday: Boolean,
        val isHoliday: Boolean,
        val isJointLeave: Boolean = false,
        val isSunday: Boolean,
        val isTrailing: Boolean = false
    )

    private val cells    = mutableListOf<CellData>()
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    private var year     = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
    private var month    = intent.getIntExtra("month", Calendar.getInstance().get(Calendar.MONTH))

    private fun refreshYearMonth() {
        val offset = ctx.getSharedPreferences("calendar_widget_fola", Context.MODE_PRIVATE)
            .getInt("offset_$widgetId", 0)
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        year  = cal.get(Calendar.YEAR)
        month = cal.get(Calendar.MONTH)
    }

    override fun onCreate() { refreshYearMonth(); load() }
    override fun onDataSetChanged() { refreshYearMonth(); load() }
    override fun onDestroy() {}

    private fun load() {
        cells.clear()
        val holidays    = HolidaysData.allHolidays
        val tmpCal      = Calendar.getInstance().apply { set(year, month, 1) }
        val today       = Calendar.getInstance()
        var firstDow    = tmpCal.get(Calendar.DAY_OF_WEEK) - 1
        if (firstDow < 0) firstDow = 6

        if (firstDow > 0) {
            val prevCal = Calendar.getInstance().apply {
                set(year, month, 1)
                add(Calendar.DAY_OF_MONTH, -firstDow)
            }
            repeat(firstDow) {
                val d   = prevCal.get(Calendar.DAY_OF_MONTH)
                val m   = prevCal.get(Calendar.MONTH)
                val y   = prevCal.get(Calendar.YEAR)
                val dow = prevCal.get(Calendar.DAY_OF_WEEK)
                val key = String.format("%04d-%02d-%02d", y, m + 1, d)
                val entry = holidays[key]
                cells.add(CellData(d, false, entry != null, entry?.type == HolidayType.JOINT_LEAVE, dow == Calendar.SUNDAY, isTrailing = true))
                prevCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val daysInMonth = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            tmpCal.set(year, month, day)
            val dow     = tmpCal.get(Calendar.DAY_OF_WEEK)
            val key     = String.format("%04d-%02d-%02d", year, month + 1, day)
            val entry   = holidays[key]
            val isToday = year == today.get(Calendar.YEAR)
                    && month == today.get(Calendar.MONTH)
                    && day == today.get(Calendar.DAY_OF_MONTH)
            cells.add(CellData(day, isToday, entry != null, entry?.type == HolidayType.JOINT_LEAVE, dow == Calendar.SUNDAY))
        }

        val nextCal = Calendar.getInstance().apply {
            set(year, month, daysInMonth)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        while (cells.size < 42) {
            val d   = nextCal.get(Calendar.DAY_OF_MONTH)
            val m   = nextCal.get(Calendar.MONTH)
            val y   = nextCal.get(Calendar.YEAR)
            val dow = nextCal.get(Calendar.DAY_OF_WEEK)
            val key = String.format("%04d-%02d-%02d", y, m + 1, d)
            val entry = holidays[key]
            cells.add(CellData(d, false, entry != null, entry?.type == HolidayType.JOINT_LEAVE, dow == Calendar.SUNDAY, isTrailing = true))
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    override fun getCount() = cells.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= cells.size) return RemoteViews(ctx.packageName, R.layout.widget_day_cell_normal)
        val cell = cells[position]
        if (cell.day == 0) return RemoteViews(ctx.packageName, R.layout.widget_day_cell_normal)

        val layout = if (cell.isTrailing) {
            when {
                cell.isJointLeave -> R.layout.widget_day_cell_trailing_joint_leave
                cell.isHoliday    -> R.layout.widget_day_cell_trailing_holiday
                cell.isSunday     -> R.layout.widget_day_cell_trailing_sunday
                else              -> R.layout.widget_day_cell_trailing
            }
        } else {
            when {
                cell.isToday      -> R.layout.widget_day_cell_today
                cell.isJointLeave -> R.layout.widget_day_cell_joint_leave
                cell.isHoliday    -> R.layout.widget_day_cell_holiday
                cell.isSunday     -> R.layout.widget_day_cell_sunday
                else              -> R.layout.widget_day_cell_normal
            }
        }

        return RemoteViews(ctx.packageName, layout).also {
            it.setTextViewText(R.id.widget_tv_day, cell.day.toString())
            it.setOnClickFillInIntent(R.id.widget_tv_day, Intent())
        }
    }

    override fun getLoadingView()    = null
    override fun getViewTypeCount()  = 8
    override fun getItemId(pos: Int) = pos.toLong()
    override fun hasStableIds()      = true
}