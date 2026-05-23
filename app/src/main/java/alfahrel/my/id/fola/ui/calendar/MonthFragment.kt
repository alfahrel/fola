package alfahrel.my.id.fola.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.ui.sheet.DaySheet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MonthFragment : Fragment() {

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): MonthFragment {
            val fragment = MonthFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    private var taskDates: Set<String> = emptySet()
    private var hasNoDateTask: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_month, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTasksAndCalendar()
    }

    fun refreshToday() {
        loadTasksAndCalendar()
    }

    private fun loadTasksAndCalendar() {
        val position = arguments?.getInt(ARG_POSITION) ?: CalendarActivity.START_POSITION
        val cal      = (requireActivity() as CalendarActivity).pageToCalendar(position)
        val year     = cal.get(Calendar.YEAR)
        val month    = cal.get(Calendar.MONTH)

        val db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            val startCal = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(year, month + 1, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val datedTasks  = withContext(Dispatchers.IO) {
                db.taskDao().getTasksForDay(startCal.timeInMillis, endCal.timeInMillis)
            }
            val noDateTasks = withContext(Dispatchers.IO) {
                db.taskDao().getActiveTasksForDay(0L, Long.MAX_VALUE)
                    .filter { it.startDateMs == null && it.dueDateMs == null }
            }

            hasNoDateTask = noDateTasks.isNotEmpty()

            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            taskDates = datedTasks.flatMap { task ->
                val dates = mutableListOf<String>()
                task.dueDateMs?.let   { dates.add(fmt.format(Date(it))) }
                task.startDateMs?.let { dates.add(fmt.format(Date(it))) }
                dates
            }.toSet()

            withContext(Dispatchers.Main) {
                buildAndSetAdapter(year, month, todayStart)
            }
        }
    }

    private fun buildAndSetAdapter(year: Int, month: Int, todayStart: Long) {
        view?.findViewById<RecyclerView>(R.id.rvCalendar)?.also {
            it.layoutManager = GridLayoutManager(requireContext(), 7)
            it.itemAnimator  = null
            it.adapter       = CalendarAdapter(buildDayList(year, month, todayStart)) { day ->
                openDaySheet(day, year, month)
            }
        }
    }

    private fun openDaySheet(day: CalendarDay, year: Int, month: Int) {
        val db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            val cal     = Calendar.getInstance().apply {
                set(year, month, day.day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs     = cal.timeInMillis
            val endMs       = startMs + 86_400_000L
            val todayStart  = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val datedTasks  = withContext(Dispatchers.IO) {
                db.taskDao().getTasksForDay(startMs, endMs)
            }
            val noDateTasks = withContext(Dispatchers.IO) {
                db.taskDao().getActiveTasksForDay(0L, Long.MAX_VALUE)
                    .filter { it.startDateMs == null && it.dueDateMs == null }
            }

            val tasks = if (startMs >= todayStart) {
                (datedTasks + noDateTasks).distinctBy { it.id }
            } else {
                datedTasks
            }

            withContext(Dispatchers.Main) {
                DaySheet.newInstance(day, tasks).show(parentFragmentManager, "day_sheet")
            }
        }
    }

    private fun getHolidays() = (requireActivity() as CalendarActivity).getCurrentHolidays()

    private fun buildDayList(year: Int, month: Int, todayStart: Long): List<CalendarDay> {
        val holidays = getHolidays()
        val days     = mutableListOf<CalendarDay>()
        val tmpCal   = Calendar.getInstance().apply { set(year, month, 1) }
        val today    = Calendar.getInstance()
        val fmt      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var firstDow = tmpCal.get(Calendar.DAY_OF_WEEK) - 1
        if (firstDow < 0) firstDow = 6

        val prevCal = Calendar.getInstance().apply {
            set(year, month, 1)
            add(Calendar.DAY_OF_MONTH, -firstDow)
        }
        repeat(firstDow) {
            val d      = prevCal.get(Calendar.DAY_OF_MONTH)
            val m      = prevCal.get(Calendar.MONTH)
            val y      = prevCal.get(Calendar.YEAR)
            val dow    = prevCal.get(Calendar.DAY_OF_WEEK)
            val key    = String.format("%04d-%02d-%02d", y, m + 1, d)
            val entry  = holidays[key]
            val dayStart = (prevCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val taskKey  = fmt.format(prevCal.time)
            val hasTask  = taskKey in taskDates || (hasNoDateTask && dayStart.timeInMillis >= todayStart)
            days.add(CalendarDay(d, false, entry != null, dow == Calendar.SUNDAY, entry?.name, entry?.description, entry?.type, isTrailing = true, hasTask = hasTask))
            prevCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInMonth = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            tmpCal.set(year, month, day)
            val dow      = tmpCal.get(Calendar.DAY_OF_WEEK)
            val key      = String.format("%04d-%02d-%02d", year, month + 1, day)
            val entry    = holidays[key]
            val isToday  = year == today.get(Calendar.YEAR)
                    && month == today.get(Calendar.MONTH)
                    && day == today.get(Calendar.DAY_OF_MONTH)
            val taskKey  = fmt.format(tmpCal.time)
            val dayStart = (tmpCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val hasTask  = taskKey in taskDates || (hasNoDateTask && dayStart >= todayStart)
            days.add(CalendarDay(day, isToday, entry != null, dow == Calendar.SUNDAY, entry?.name, entry?.description, entry?.type,
                hasTask = hasTask))
        }

        val nextCal = Calendar.getInstance().apply {
            set(year, month, daysInMonth)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        while (days.size < 42) {
            val d      = nextCal.get(Calendar.DAY_OF_MONTH)
            val m      = nextCal.get(Calendar.MONTH)
            val y      = nextCal.get(Calendar.YEAR)
            val dow    = nextCal.get(Calendar.DAY_OF_WEEK)
            val key    = String.format("%04d-%02d-%02d", y, m + 1, d)
            val entry  = holidays[key]
            val dayStart = (nextCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val taskKey  = fmt.format(nextCal.time)
            val hasTask  = taskKey in taskDates || (hasNoDateTask && dayStart.timeInMillis >= todayStart)
            days.add(CalendarDay(d, false, entry != null, dow == Calendar.SUNDAY, entry?.name, entry?.description, entry?.type, isTrailing = true, hasTask = hasTask))
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }
}
