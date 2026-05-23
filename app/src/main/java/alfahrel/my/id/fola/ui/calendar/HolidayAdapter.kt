package alfahrel.my.id.fola.ui.calendar

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.model.Task
import com.google.android.material.card.MaterialCardView
import com.google.android.material.R as MatR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DayEvent {
    data class Holiday(val info: HolidayInfo) : DayEvent()
    data class TaskEvent(val task: Task, val dateMs: Long) : DayEvent()
}

class HolidayAdapter(private val events: List<DayEvent>) :
    RecyclerView.Adapter<HolidayAdapter.EventVH>() {

    private val interpolator      = DecelerateInterpolator()
    private val expandedPositions = mutableSetOf<Int>()

    inner class EventVH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView        = view as MaterialCardView
        val tvDate: TextView              = view.findViewById(R.id.tvHolidayDate)
        val tvName: TextView              = view.findViewById(R.id.tvHolidayName)
        val tvDesc: TextView              = view.findViewById(R.id.tvHolidayDesc)
        val tvEllipsis: TextView          = view.findViewById(R.id.ivChevron)
        val layoutExpanded: LinearLayout  = view.findViewById(R.id.layoutExpanded)
        val layoutCollapsed: LinearLayout = view.findViewById(R.id.layoutCollapsed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        EventVH(LayoutInflater.from(parent.context).inflate(R.layout.item_holiday, parent, false))

    override fun getItemCount() = events.size

    override fun onBindViewHolder(h: EventVH, position: Int) {
        val ctx        = h.itemView.context
        val monthShort = ctx.resources.getStringArray(R.array.month_names_short)

        when (val event = events[position]) {
            is DayEvent.Holiday -> {
                h.tvDate.text = "${event.info.day} ${monthShort[event.info.month - 1]}"
                h.tvName.text = event.info.name
                h.tvDesc.text = event.info.description
                h.tvEllipsis.visibility = View.VISIBLE

                when (event.info.type) {
                    HolidayType.JOINT_LEAVE -> {
                        h.card.setCardBackgroundColor(ctx.resolveAttrColor(MatR.attr.colorOnError))
                        h.tvDate.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                        h.tvName.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                        h.tvDesc.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                        h.tvEllipsis.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                    }
                    HolidayType.NATIONAL,
                    HolidayType.RELIGIOUS -> {
                        h.card.setCardBackgroundColor(ctx.resolveAttrColor(MatR.attr.colorErrorContainer))
                        h.tvDate.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                        h.tvName.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                        h.tvDesc.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                        h.tvEllipsis.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnErrorContainer))
                    }
                }
            }
            is DayEvent.TaskEvent -> {
                h.card.setCardBackgroundColor(ctx.resolveAttrColor(MatR.attr.colorSurfaceContainerHigh))
                h.tvDate.setTextColor(ctx.resolveAttrColor(MatR.attr.colorPrimaryVariant))
                h.tvName.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnSurface))
                h.tvDesc.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnSurfaceVariant))
                h.tvEllipsis.setTextColor(ctx.resolveAttrColor(MatR.attr.colorOnSurfaceVariant))

                val date      = Date(event.dateMs)
                val cal       = java.util.Calendar.getInstance().apply { time = date }
                val day       = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val month     = cal.get(java.util.Calendar.MONTH)
                h.tvDate.text = "$day ${monthShort[month]}"
                h.tvName.text = event.task.title

                val fmt       = SimpleDateFormat("MMM d", Locale.getDefault())
                val start     = event.task.startDateMs
                val due       = event.task.dueDateMs
                val descParts = mutableListOf<String>()
                descParts.add(event.task.category)
                when {
                    start != null && due != null -> descParts.add("${fmt.format(Date(start))} – ${fmt.format(Date(due))}")
                    due   != null                -> descParts.add("Due ${fmt.format(Date(due))}")
                    start != null                -> descParts.add("Starts ${fmt.format(Date(start))}")
                }
                if (event.task.isRepeat) descParts.add("Repeating")
                h.tvDesc.text           = descParts.joinToString(" · ")
                h.tvEllipsis.visibility = View.VISIBLE
            }
        }

        val isExpanded = position in expandedPositions
        h.layoutExpanded.visibility          = if (isExpanded) View.VISIBLE else View.GONE
        h.layoutExpanded.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        h.tvEllipsis.alpha                   = if (isExpanded) 0f else 1f

        h.layoutCollapsed.setOnClickListener {
            val pos = h.bindingAdapterPosition
            if (pos == RecyclerView.NO_ID.toInt()) return@setOnClickListener
            if (pos in expandedPositions) {
                expandedPositions.remove(pos)
                collapseView(h.layoutExpanded, h.tvEllipsis)
            } else {
                expandedPositions.add(pos)
                expandView(h.layoutExpanded, h.tvEllipsis)
            }
        }

        h.itemView.alpha        = 0f
        h.itemView.translationY = 60f
        h.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setStartDelay(position * 80L)
            .setInterpolator(interpolator)
            .start()
    }

    private fun expandView(target: LinearLayout, ellipsis: TextView) {
        target.measure(
            View.MeasureSpec.makeMeasureSpec((target.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight             = target.measuredHeight
        target.layoutParams.height   = 0
        target.visibility            = View.VISIBLE

        ValueAnimator.ofInt(0, targetHeight).apply {
            duration     = 250
            interpolator = this@HolidayAdapter.interpolator
            addUpdateListener {
                target.layoutParams.height = it.animatedValue as Int
                target.requestLayout()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(a: Animator)    { target.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT }
                override fun onAnimationStart(a: Animator)  {}
                override fun onAnimationCancel(a: Animator) {}
                override fun onAnimationRepeat(a: Animator) {}
            })
            start()
        }
        ellipsis.animate().alpha(0f).setDuration(250).setInterpolator(interpolator).start()
    }

    private fun collapseView(target: LinearLayout, ellipsis: TextView) {
        val initialHeight = target.measuredHeight

        ValueAnimator.ofInt(initialHeight, 0).apply {
            duration     = 250
            interpolator = this@HolidayAdapter.interpolator
            addUpdateListener {
                target.layoutParams.height = it.animatedValue as Int
                target.requestLayout()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(a: Animator) {
                    target.visibility          = View.GONE
                    target.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                override fun onAnimationStart(a: Animator)  {}
                override fun onAnimationCancel(a: Animator) {}
                override fun onAnimationRepeat(a: Animator) {}
            })
            start()
        }
        ellipsis.animate().alpha(1f).setDuration(250).setInterpolator(interpolator).start()
    }

    override fun onViewRecycled(h: EventVH) {
        super.onViewRecycled(h)
        h.itemView.animate().cancel()
        h.itemView.alpha        = 1f
        h.itemView.translationY = 0f
    }
}

private fun Context.resolveAttrColor(attr: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(attr, tv, true)
    return tv.data
}
