package alfahrel.my.id.fola.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.model.RepeatType
import alfahrel.my.id.fola.data.model.Task
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.color.MaterialColors
import java.util.Calendar
import java.util.Date

class TaskAdapter(
    private val onToggle: (Task, Boolean) -> Unit,
    private val onLongClick: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onEdit: (Task) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rawActive:      List<Task> = emptyList()
    private var rawCompleted:   List<Task> = emptyList()
    private var activeTasks:    List<Task> = emptyList()
    private var completedTasks: List<Task> = emptyList()
    private var categoryFilter: String?    = null
    private var dateFilter: Int?           = null

    companion object {
        private const val TYPE_TASK         = 0
        private const val TYPE_HEADER       = 1
        private const val CORNER_RADIUS_DP  = 33f
    }

    fun setDateFilter(daysAhead: Int?) {
        dateFilter = daysAhead
        applyFilter()
    }

    fun isEmpty() = activeTasks.isEmpty() && completedTasks.isEmpty()

    fun submitActive(list: List<Task>) {
        rawActive = list
        applyFilter()
    }

    fun submitCompleted(list: List<Task>) {
        rawCompleted = list
        applyFilter()
    }

    fun setFilter(category: String?) {
        categoryFilter = category
        applyFilter()
    }

    private fun applyFilter() {
        var active    = if (categoryFilter == null) rawActive    else rawActive.filter    { it.category == categoryFilter }
        var completed = if (categoryFilter == null) rawCompleted else rawCompleted.filter { it.category == categoryFilter }

        dateFilter?.let { days ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.timeInMillis

            // "Today"    → [startOfDay,   startOfDay + 1d)
            // "Tomorrow" → [startOfDay+1d, startOfDay + 2d)
            // "This Week"→ [startOfDay,   startOfDay + 7d)
            val windowStart = when (days) {
                1    -> startOfToday + 86_400_000L          // tomorrow only
                else -> startOfToday                        // today or this-week
            }
            val windowEnd = when (days) {
                0    -> startOfToday + 86_400_000L           // today:      +1d
                1    -> startOfToday + 2 * 86_400_000L       // tomorrow:   +2d
                else -> startOfToday + (days + 1) * 86_400_000L // this week: +8d
            }

            // A task matches if ANY of its relevant dates fall in the window.
            // Tasks with no dates at all are excluded when a date filter is active.
            fun Task.matchesWindow(): Boolean {
                val due   = dueDateMs
                val start = startDateMs
                return when {
                    due   != null && due   in windowStart until windowEnd -> true
                    start != null && start in windowStart until windowEnd -> true
                    else -> false
                }
            }

            active    = active.filter    { it.matchesWindow() }
            completed = completed.filter { it.matchesWindow() }
        }

        activeTasks    = active
        completedTasks = completed
        notifyDataSetChanged()
    }

    fun activeCount()    = rawActive.size
    fun completedCount() = rawCompleted.size

    fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                if (vh is HeaderViewHolder) return 0
                return super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val task = getItem(vh.bindingAdapterPosition)
                if (direction == ItemTouchHelper.LEFT) {
                    onDelete(task)
                } else {
                    notifyItemChanged(vh.bindingAdapterPosition)
                    onEdit(task)
                }
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isActive: Boolean
            ) {
                val item    = vh.itemView
                val context = rv.context
                val density = context.resources.displayMetrics.density
                val radius  = CORNER_RADIUS_DP * density

                val isSwipingRight = dX > 0

                val bgColor = if (isSwipingRight)
                    MaterialColors.getColor(rv, com.google.android.material.R.attr.colorSecondaryContainer)
                else
                    MaterialColors.getColor(rv, com.google.android.material.R.attr.colorErrorContainer)

                val iconTint = if (isSwipingRight)
                    MaterialColors.getColor(rv, com.google.android.material.R.attr.colorOnSecondaryContainer)
                else
                    MaterialColors.getColor(rv, com.google.android.material.R.attr.colorOnErrorContainer)

                val iconRes = if (isSwipingRight) R.drawable.ic_rounded_edit_24 else R.drawable.ic_rounded_delete_24

                val swipeRatio = (Math.abs(dX) / item.width).coerceIn(0f, 1f)
                val paint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }

                val rect = if (isSwipingRight) {
                    RectF(item.left.toFloat(), item.top.toFloat(), item.left + dX, item.bottom.toFloat())
                } else {
                    RectF(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat())
                }

                val path = android.graphics.Path().apply {
                    addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
                }
                c.drawPath(path, paint)

                val icon = ContextCompat.getDrawable(context, iconRes)
                if (icon != null && swipeRatio > 0.1f) {
                    icon.setTint(iconTint)
                    val iconSize   = icon.intrinsicHeight
                    val iconMargin = (item.height - iconSize) / 2
                    val iconTop    = item.top + iconMargin
                    val iconBottom = iconTop + iconSize

                    if (isSwipingRight) {
                        val iconLeft = item.left + iconMargin
                        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconBottom)
                    } else {
                        val iconRight = item.right - iconMargin
                        icon.setBounds(iconRight - iconSize, iconTop, iconRight, iconBottom)
                    }
                    icon.draw(c)
                }

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int {
        var count = activeTasks.size
        if (completedTasks.isNotEmpty()) count += 1 + completedTasks.size
        return count
    }

    override fun getItemViewType(position: Int): Int =
        if (completedTasks.isNotEmpty() && position == activeTasks.size) TYPE_HEADER else TYPE_TASK

    private fun getItem(position: Int): Task =
        if (position < activeTasks.size) activeTasks[position]
        else completedTasks[position - activeTasks.size - 1]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false))
        } else {
            TaskViewHolder(inflater.inflate(R.layout.item_task, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind("Completed (${completedTasks.size})")
            is TaskViewHolder   -> holder.bind(getItem(position))
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvSectionHeader)
        fun bind(title: String) { tvHeader.text = title }
    }

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val check:        MaterialCheckBox = view.findViewById(R.id.checkTask)
        private val tvTitle:      TextView         = view.findViewById(R.id.tvTaskTitle)
        private val tvDateRange:  TextView         = view.findViewById(R.id.tvTaskDateRange)
        private val tvCategory:   TextView         = view.findViewById(R.id.tvTaskCategory)
        private val tvRepeat:     TextView         = view.findViewById(R.id.tvTaskRepeat)

        fun bind(task: Task) {
            tvTitle.text    = task.title
            tvCategory.text = task.category

            if (task.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.4f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1f
            }

            val fmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())

            val startText = task.startDateMs?.let { "Starts ${fmt.format(Date(it))}" }
            val dueText   = task.dueDateMs?.let { "Due ${fmt.format(Date(it))}" }

            val dateRangeText = listOfNotNull(startText, dueText).joinToString(" • ")
            if (dateRangeText.isNotEmpty()) {
                tvDateRange.visibility = View.VISIBLE
                tvDateRange.text = dateRangeText
            } else {
                tvDateRange.visibility = View.GONE
            }

            if (task.isRepeat) {
                tvRepeat.visibility = View.VISIBLE
                tvRepeat.text = when (task.repeatType) {
                    RepeatType.DAILY   -> "Every day"
                    RepeatType.WEEKLY  -> "Every week"
                    RepeatType.MONTHLY -> "Every month"
                    RepeatType.CUSTOM  -> "Every ${task.repeatInterval} ${task.repeatUnit}"
                }
            } else {
                tvRepeat.visibility = View.GONE
            }

            check.setOnCheckedChangeListener(null)
            check.isChecked = task.isCompleted
            check.setOnCheckedChangeListener { _, checked -> onToggle(task, checked) }
            itemView.setOnLongClickListener { onLongClick(task); true }
        }
    }
}