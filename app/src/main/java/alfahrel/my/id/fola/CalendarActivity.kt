package alfahrel.my.id.fola

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import alfahrel.my.id.fola.calendar.CalendarAdapter
import alfahrel.my.id.fola.calendar.HolidayAdapter
import alfahrel.my.id.fola.calendar.HolidayInfo
import alfahrel.my.id.fola.calendar.MonthFragment
import alfahrel.my.id.fola.calendar.MonthPagerAdapter
import alfahrel.my.id.fola.calendar.HolidaysData
import alfahrel.my.id.fola.widget.FolaWidgetProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class CalendarActivity : BaseActivity() {

    companion object {
        const val START_POSITION = 1200
    }

    private lateinit var toolbar: Toolbar
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var rvHolidays: RecyclerView
    private lateinit var tvNoHoliday: LinearLayout
    private lateinit var fabCurrentMonth: FloatingActionButton

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MidnightWorker.ACTION_DATE_CHANGED) {
                supportFragmentManager.fragments.forEach { fragment ->
                    if (fragment is MonthFragment) fragment.refreshToday()
                }
                val cal = pageToCalendar(viewPager.currentItem)
                updateHolidays(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        toolbar         = findViewById(R.id.toolbar)
        tvMonthYear     = findViewById(R.id.tvMonthYear)
        btnPrev         = findViewById(R.id.btnPrev)
        btnNext         = findViewById(R.id.btnNext)
        viewPager       = findViewById(R.id.viewPager)
        rvHolidays      = findViewById(R.id.rvHolidays)
        tvNoHoliday     = findViewById(R.id.tvNoHoliday)
        fabCurrentMonth = findViewById(R.id.fabCurrentMonth)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvHolidays.layoutManager = LinearLayoutManager(this)
        rvHolidays.itemAnimator  = null

        viewPager.adapter = MonthPagerAdapter(this)
        viewPager.setCurrentItem(START_POSITION, false)

        val initialCal   = pageToCalendar(START_POSITION)
        val initialYear  = initialCal.get(Calendar.YEAR)
        val initialMonth = initialCal.get(Calendar.MONTH)
        val monthNames   = resources.getStringArray(R.array.month_names)

        tvMonthYear.text = monthNames[initialMonth]
        updateHolidays(initialYear, initialMonth)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val cal   = pageToCalendar(position)
                val year  = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                val names = resources.getStringArray(R.array.month_names)
                tvMonthYear.text = names[month]
                updateHolidays(year, month)
            }
        })

        viewPager.setPageTransformer { page, _ ->
            page.parent.requestDisallowInterceptTouchEvent(true)
        }

        btnPrev.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem - 1, true) }
        btnNext.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem + 1, true) }
        fabCurrentMonth.setOnClickListener { viewPager.setCurrentItem(START_POSITION, true) }

        MidnightWorker.scheduleMidnightWork(this)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            dateChangeReceiver,
            IntentFilter(MidnightWorker.ACTION_DATE_CHANGED),
            Context.RECEIVER_NOT_EXPORTED
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

    private fun updateHolidays(year: Int, month: Int) {
        val holidays = getHolidaysForMonth(year, month)
        if (holidays.isEmpty()) {
            rvHolidays.visibility  = View.GONE
            tvNoHoliday.visibility = View.VISIBLE
            tvNoHoliday.alpha = 0f
            tvNoHoliday.translationY = 40f
            tvNoHoliday.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(100).start()
        } else {
            rvHolidays.visibility  = View.VISIBLE
            tvNoHoliday.visibility = View.GONE
            rvHolidays.adapter = HolidayAdapter(holidays)
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