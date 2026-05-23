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
import android.view.Menu
import android.view.MenuItem
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class CalendarActivity : BaseActivity() {

    companion object {
        const val START_POSITION            = 1200
        private const val HOLIDAY_DATA_YEAR = 2026
        private const val PREFS_NAME        = "calendar_prefs"
        private const val KEY_SHOW_HOLIDAYS = "show_holidays"
        private const val KEY_SHOW_TASKS    = "show_tasks"
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

    private var showHolidays: Boolean = false
    private var showTasks: Boolean    = true
    private var menuItemToggle: MenuItem? = null
    private var menuItemTasks: MenuItem?  = null

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

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

        showHolidays = prefs.getBoolean(KEY_SHOW_HOLIDAYS, false)
        showTasks    = prefs.getBoolean(KEY_SHOW_TASKS, true)

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
                if (year != HOLIDAY_DATA_YEAR) {
                    showHolidayLimitDialog()
                    return
                }
                updateMonthYear(year, month)
                updateEvents(year, month)
            }
        })

        btnPrev.setOnClickListener {
            val nextPos = viewPager.currentItem - 1
            val cal     = pageToCalendar(nextPos)
            if (cal.get(Calendar.YEAR) != HOLIDAY_DATA_YEAR) {
                showHolidayLimitDialog()
            } else {
                viewPager.setCurrentItem(nextPos, true)
            }
        }
        btnNext.setOnClickListener {
            val nextPos = viewPager.currentItem + 1
            val cal     = pageToCalendar(nextPos)
            if (cal.get(Calendar.YEAR) != HOLIDAY_DATA_YEAR) {
                showHolidayLimitDialog()
            } else {
                viewPager.setCurrentItem(nextPos, true)
            }
        }
        fabCurrentMonth.setOnClickListener { viewPager.setCurrentItem(START_POSITION, true) }

        MidnightWorker.Companion.scheduleMidnightWork(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_calendar, menu)
        menuItemToggle = menu.findItem(R.id.action_toggle_holiday)
        menuItemTasks  = menu.findItem(R.id.action_toggle_tasks)
        menuItemToggle?.isChecked = showHolidays
        menuItemTasks?.isChecked  = showTasks
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_holiday -> {
                val newValue = !showHolidays
                MaterialAlertDialogBuilder(this)
                    .setTitle(if (newValue) R.string.dialog_show_holiday_title_on else R.string.dialog_show_holiday_title_off)
                    .setMessage(R.string.dialog_show_holiday_message)
                    .setPositiveButton(R.string.dialog_show_holiday_btn_restart) { _, _ ->
                        prefs.edit().putBoolean(KEY_SHOW_HOLIDAYS, newValue).apply()
                        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton(R.string.dialog_show_holiday_btn_cancel, null)
                    .show()
                true
            }
            R.id.action_toggle_tasks -> {
                showTasks = !showTasks
                item.isChecked = showTasks
                prefs.edit().putBoolean(KEY_SHOW_TASKS, showTasks).apply()
                val cal = pageToCalendar(viewPager.currentItem)
                updateEvents(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                true
            }
            R.id.action_holiday_info -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_holiday_info_title)
                    .setMessage(R.string.dialog_holiday_info_message)
                    .setPositiveButton(R.string.dialog_holiday_info_btn, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    fun getCurrentHolidays() = if (showHolidays) HolidaysData.allHolidays else emptyMap()

    private fun showHolidayLimitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_holiday_data_title)
            .setMessage(R.string.dialog_holiday_data_message)
            .setPositiveButton(R.string.dialog_holiday_data_btn) { _, _ ->
                viewPager.setCurrentItem(START_POSITION, true)
            }
            .setCancelable(false)
            .show()
    }

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

            val tasks = if (showTasks) {
                withContext(Dispatchers.IO) {
                    db.taskDao().getTasksForDay(startCal.timeInMillis, endCal.timeInMillis)
                }
            } else {
                emptyList()
            }

            val holidays = getHolidaysForMonth(year, month)
            val events   = mutableListOf<DayEvent>()
            val tmpCal   = Calendar.getInstance()

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
                    rvHolidays.visibility    = View.GONE
                    tvNoHoliday.visibility   = View.VISIBLE
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
        if (!showHolidays || year != HOLIDAY_DATA_YEAR) return emptyList()
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
