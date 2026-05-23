package alfahrel.my.id.fola.ui.calendar

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.service.MidnightWorker
import alfahrel.my.id.fola.util.BaseActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class CalendarActivity : BaseActivity() {

    companion object {
        const val START_POSITION = 1200
    }

    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var rvHolidays: RecyclerView
    private lateinit var tvNoHoliday: LinearLayout
    private lateinit var fabCurrentMonth: FloatingActionButton
    private lateinit var rootLayout: NestedScrollView

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MidnightWorker.Companion.ACTION_DATE_CHANGED) {
                supportFragmentManager.fragments.forEach { fragment ->
                    if (fragment is MonthFragment) fragment.refreshToday()
                }
                val cal = pageToCalendar(viewPager.currentItem)
                updateEvents(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        toolbar           = findViewById(R.id.toolbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        tvMonthYear       = findViewById(R.id.tvMonthYear)
        btnPrev           = findViewById(R.id.btnPrev)
        btnNext           = findViewById(R.id.btnNext)
        viewPager         = findViewById(R.id.viewPager)
        rvHolidays        = findViewById(R.id.rvHolidays)
        tvNoHoliday       = findViewById(R.id.tvNoHoliday)
        fabCurrentMonth   = findViewById(R.id.fabCurrentMonth)
        rootLayout        = findViewById(R.id.rootLayout)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvHolidays.layoutManager = LinearLayoutManager(this)
        rvHolidays.itemAnimator  = null

        viewPager.adapter = MonthPagerAdapter(this)
        viewPager.setCurrentItem(START_POSITION, false)
        viewPager.isUserInputEnabled = false

        val initialCal   = pageToCalendar(START_POSITION)
        val initialYear  = initialCal.get(Calendar.YEAR)
        val initialMonth = initialCal.get(Calendar.MONTH)

        updateMonthYear(initialYear, initialMonth)
        updateEvents(initialYear, initialMonth)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val cal   = pageToCalendar(position)
                val year  = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                updateMonthYear(year, month)
                updateEvents(year, month)
            }
        })

        btnPrev.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem - 1, true) }
        btnNext.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem + 1, true) }
        fabCurrentMonth.setOnClickListener { viewPager.setCurrentItem(START_POSITION, true) }

        MidnightWorker.Companion.scheduleMidnightWork(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        registerReceiver(
            dateChangeReceiver,
            IntentFilter(MidnightWorker.Companion.ACTION_DATE_CHANGED),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(dateChangeReceiver)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun pageToCalendar(position: Int): Calendar {
        val offset = position - START_POSITION
        return Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
    }

    fun getCurrentHolidays() = HolidaysData.allHolidays

    private fun updateMonthYear(year: Int, month: Int) {
        val monthNames = resources.getStringArray(R.array.month_names)
        val label      = "${monthNames[month]} $year"
        collapsingToolbar.title = label
        tvMonthYear.text        = monthNames[month]
    }

    private fun updateEvents(year: Int, month: Int) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val startCal = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(year, month + 1, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val tasks = withContext(Dispatchers.IO) {
                db.taskDao().getTasksForDay(startCal.timeInMillis, endCal.timeInMillis)
            }

            val holidays     = getHolidaysForMonth(year, month)
            val events       = mutableListOf<DayEvent>()
            val tmpCal       = Calendar.getInstance()

            holidays.forEach { events.add(DayEvent.Holiday(it)) }

            tasks.forEach { task ->
                val dateMs = task.dueDateMs ?: task.startDateMs
                if (dateMs != null) {
                    tmpCal.timeInMillis = dateMs
                    val tYear  = tmpCal.get(Calendar.YEAR)
                    val tMonth = tmpCal.get(Calendar.MONTH)
                    if (tYear == year && tMonth == month) {
                        events.add(DayEvent.TaskEvent(task, dateMs))
                    }
                }
            }

            events.sortWith(compareBy {
                when (it) {
                    is DayEvent.Holiday   -> it.info.day
                    is DayEvent.TaskEvent -> {
                        tmpCal.timeInMillis = it.dateMs
                        tmpCal.get(Calendar.DAY_OF_MONTH)
                    }
                }
            })

            withContext(Dispatchers.Main) {
                if (events.isEmpty()) {
                    rvHolidays.visibility  = View.GONE
                    tvNoHoliday.visibility = View.VISIBLE
                    tvNoHoliday.alpha        = 0f
                    tvNoHoliday.translationY = 40f
                    tvNoHoliday.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(100).start()
                } else {
                    rvHolidays.visibility  = View.VISIBLE
                    tvNoHoliday.visibility = View.GONE
                    rvHolidays.adapter     = HolidayAdapter(events)
                }
            }
        }
    }

    private fun getHolidaysForMonth(year: Int, month: Int): List<HolidayInfo> {
        val holidays = HolidaysData.allHolidays
        val result   = mutableListOf<HolidayInfo>()
        val tmpCal   = Calendar.getInstance().apply { set(year, month, 1) }
        val days     = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..days) {
            val key   = String.format("%04d-%02d-%02d", year, month + 1, day)
            val entry = holidays[key] ?: continue
            result.add(HolidayInfo(day, month + 1, year, entry.name, entry.description, entry.type))
        }
        return result
    }
}
