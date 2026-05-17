package alfahrel.my.id.fola.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import alfahrel.my.id.fola.CalendarActivity
import alfahrel.my.id.fola.R
import java.util.Calendar

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_month, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCalendar()
    }

    fun refreshToday() {
        loadCalendar()
    }

    private fun loadCalendar() {
        val position = arguments?.getInt(ARG_POSITION) ?: CalendarActivity.START_POSITION
        val cal      = (requireActivity() as CalendarActivity).pageToCalendar(position)
        val year     = cal.get(Calendar.YEAR)
        val month    = cal.get(Calendar.MONTH)

        view?.findViewById<RecyclerView>(R.id.rvCalendar)?.also {
            it.layoutManager = GridLayoutManager(requireContext(), 7)
            it.itemAnimator  = null
            it.adapter       = CalendarAdapter(buildDayList(year, month))
        }
    }

    private fun getHolidays() = (requireActivity() as CalendarActivity).getCurrentHolidays()

    private fun buildDayList(year: Int, month: Int): List<CalendarDay> {
        val holidays = getHolidays()
        val days     = mutableListOf<CalendarDay>()
        val tmpCal   = Calendar.getInstance().apply { set(year, month, 1) }
        val today    = Calendar.getInstance()

        var firstDow = tmpCal.get(Calendar.DAY_OF_WEEK) - 1
        if (firstDow < 0) firstDow = 6

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
            days.add(CalendarDay(d, false, entry != null, dow == Calendar.SUNDAY, entry?.name, entry?.description, entry?.type, isTrailing = true))
            prevCal.add(Calendar.DAY_OF_MONTH, 1)
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
            days.add(CalendarDay(day, isToday, entry != null, dow == Calendar.SUNDAY, entry?.name, entry?.description, entry?.type))
        }

        val remainder = days.size % 7
        if (remainder != 0) {
            val nextCal = Calendar.getInstance().apply {
                set(year, month, daysInMonth)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            repeat(7 - remainder) {
                val d   = nextCal.get(Calendar.DAY_OF_MONTH)
                val m   = nextCal.get(Calendar.MONTH)
                val y   = nextCal.get(Calendar.YEAR)
                val dow = nextCal.get(Calendar.DAY_OF_WEEK)
                val key = String.format("%04d-%02d-%02d", y, m + 1, d)
                val entry = holidays[key]
                days.add(CalendarDay(d, false, entry != null, dow == Calendar.SUNDAY, entry?.name, entry?.description, entry?.type, isTrailing = true))
                nextCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return days
    }
}