package alfahrel.my.id.fola.ui.calendar

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import alfahrel.my.id.fola.R
import com.google.android.material.R as MatR

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayVH>() {

    inner class DayVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DayVH(LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false))

    override fun getItemCount() = days.size

    override fun onBindViewHolder(h: DayVH, position: Int) {
        val item = days[position]
        val ctx  = h.itemView.context

        if (item.day == 0) {
            h.tvDay.text       = ""
            h.tvDay.background = null
            h.tvDay.alpha      = 1f
            return
        }

        h.tvDay.text = item.day.toString()

        h.itemView.setOnClickListener {
            if (!item.isTrailing) onDayClick(item)
        }

        if (item.isTrailing) {
            h.tvDay.alpha = 0.4f
            val isJoint   = item.holidayType == HolidayType.JOINT_LEAVE
            val isHoliday = item.isHoliday && !isJoint
            when {
                item.isSunday && item.hasTask -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_sunday_task)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
                }
                item.isSunday -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_sunday)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
                }
                isJoint && item.hasTask -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_joint_task)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                }
                isJoint -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_joint)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                }
                isHoliday && item.hasTask -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_holiday_task)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
                }
                isHoliday -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_holiday)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
                }
                item.hasTask -> {
                    h.tvDay.setBackgroundResource(R.drawable.bg_task)
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorPrimaryVariant))
                }
                else -> {
                    h.tvDay.background = null
                    h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnSurface))
                }
            }
            return
        }

        h.tvDay.alpha = 1f

        val isJoint   = item.holidayType == HolidayType.JOINT_LEAVE
        val isHoliday = item.isHoliday && !isJoint

        when {
            item.isToday && item.isSunday && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_sunday_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday && item.isSunday -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_sunday)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday && isJoint && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_joint_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday && isJoint -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_joint)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday && isHoliday && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_holiday_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday && isHoliday -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_holiday)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            item.isToday -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_today)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnPrimary))
            }
            isJoint && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_joint_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
            }
            isJoint -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_joint)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
            }
            isHoliday && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_holiday_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
            }
            isHoliday -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_holiday)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
            }
            item.isSunday && item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_sunday_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
            }
            item.isSunday -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_sunday)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
            }
            item.hasTask -> {
                h.tvDay.setBackgroundResource(R.drawable.bg_task)
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorPrimaryVariant))
            }
            else -> {
                h.tvDay.background = null
                h.tvDay.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnSurface))
            }
        }
    }
}

private fun Context.resolveAttrColor(attr: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(attr, tv, true)
    return tv.data
}
