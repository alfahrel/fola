package alfahrel.my.id.fola.ui.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.model.Task
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.ui.calendar.CalendarDay
import alfahrel.my.id.fola.ui.calendar.HolidayType
import alfahrel.my.id.fola.ui.task.TaskAdapter
import kotlinx.coroutines.launch

class DaySheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_HOL_NAME = "arg_hol_name"
        private const val ARG_HOL_DESC = "arg_hol_desc"
        private const val ARG_HOL_TYPE = "arg_hol_type"

        var taskCache: List<Task> = emptyList()

        fun newInstance(day: CalendarDay, tasks: List<Task>): DaySheet {
            taskCache = tasks
            return DaySheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOL_NAME, day.holidayName)
                    putString(ARG_HOL_DESC, day.holidayDesc)
                    day.holidayType?.let { putString(ARG_HOL_TYPE, it.name) }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.sheet_day, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args        = requireArguments()
        val holidayName = args.getString(ARG_HOL_NAME)
        val holidayDesc = args.getString(ARG_HOL_DESC)
        val holidayType = args.getString(ARG_HOL_TYPE)?.let { HolidayType.valueOf(it) }

        val sectionHoliday = view.findViewById<LinearLayout>(R.id.sectionHoliday)
        val tvHolidayBadge = view.findViewById<TextView>(R.id.tvHolidayBadge)
        val tvHolidayName  = view.findViewById<TextView>(R.id.tvHolidayName)
        val tvHolidayDesc  = view.findViewById<TextView>(R.id.tvHolidayDesc)

        if (holidayName != null) {
            sectionHoliday.visibility = View.VISIBLE
            tvHolidayName.text        = holidayName
            tvHolidayDesc.text        = holidayDesc
            tvHolidayBadge.text = when (holidayType) {
                HolidayType.JOINT_LEAVE -> "Cuti Bersama"
                HolidayType.RELIGIOUS   -> "Hari Keagamaan"
                HolidayType.NATIONAL    -> "Hari Nasional"
                null                    -> ""
            }
        } else {
            sectionHoliday.visibility = View.GONE
        }

        val sectionTasks = view.findViewById<LinearLayout>(R.id.sectionTasks)
        val tvNoTasks    = view.findViewById<TextView>(R.id.tvNoTasks)
        val rvTasks      = view.findViewById<RecyclerView>(R.id.rvDayTasks)
        val tasks        = taskCache

        if (tasks.isEmpty()) {
            tvNoTasks.visibility    = View.VISIBLE
            sectionTasks.visibility = View.VISIBLE
            rvTasks.visibility      = View.GONE
        } else {
            sectionTasks.visibility = View.VISIBLE
            tvNoTasks.visibility    = View.GONE
            rvTasks.visibility      = View.VISIBLE

            val db      = AppDatabase.getInstance(requireContext())
            val adapter = TaskAdapter(
                onToggle    = { task, checked ->
                    lifecycleScope.launch { db.taskDao().setCompleted(task.id, checked) }
                },
                onLongClick = { },
                onDelete    = { task ->
                    lifecycleScope.launch { db.taskDao().delete(task) }
                },
                onEdit      = { task ->
                    EditSheet(task) { updated ->
                        lifecycleScope.launch { db.taskDao().update(updated) }
                    }.show(parentFragmentManager, "edit_sheet")
                }
            )
            rvTasks.layoutManager = LinearLayoutManager(requireContext())
            rvTasks.itemAnimator  = null
            rvTasks.adapter       = adapter
            adapter.submitActive(tasks.filter { !it.isCompleted })
            adapter.submitCompleted(tasks.filter { it.isCompleted })
        }
    }
}
