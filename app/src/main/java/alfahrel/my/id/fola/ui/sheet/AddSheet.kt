package alfahrel.my.id.fola.ui.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.model.RepeatType
import alfahrel.my.id.fola.data.model.Task
import java.text.SimpleDateFormat
import java.util.Locale

class AddSheet(
    private val onSaveTask: (Task) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedDateMs: Long? = null
    private var selectedCategory = "Task"
    private var isRepeat = false
    private var selectedRepeat = RepeatType.DAILY
    private var selectedUnit = "days"
    private var selectedStartDateMs: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_add, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tilTitle          = view.findViewById<TextInputLayout>(R.id.tilTitle)
        val etTitle           = view.findViewById<TextInputEditText>(R.id.etTitle)
        val etInterval        = view.findViewById<TextInputEditText>(R.id.etInterval)
        val layoutTaskOptions = view.findViewById<LinearLayout>(R.id.layoutTaskOptions)
        val layoutHabit       = view.findViewById<LinearLayout>(R.id.layoutHabitOptions)
        val layoutCustom      = view.findViewById<LinearLayout>(R.id.layoutCustomRepeat)

        val btnCatTask  = view.findViewById<MaterialButton>(R.id.btnCatTask)
        val btnCatHabit = view.findViewById<MaterialButton>(R.id.btnCatHabit)
        val catBtns     = listOf(btnCatTask, btnCatHabit)

        fun selectCategory(chosen: MaterialButton) {
            catBtns.forEach { it.isChecked = it === chosen }
            when (chosen) {
                btnCatTask -> {
                    selectedCategory             = "Task"
                    layoutTaskOptions.visibility = View.VISIBLE
                    layoutHabit.visibility       = View.GONE
                    isRepeat                     = false
                }
                btnCatHabit -> {
                    selectedCategory             = "Habit"
                    layoutTaskOptions.visibility = View.GONE
                    layoutHabit.visibility       = View.VISIBLE
                    isRepeat                     = true
                }
            }
        }

        catBtns.forEach { it.isCheckable = true }
        selectCategory(btnCatTask)
        btnCatTask.setOnClickListener  { selectCategory(btnCatTask)  }
        btnCatHabit.setOnClickListener { selectCategory(btnCatHabit) }

        val btnPickStartDate = view.findViewById<MaterialButton>(R.id.btnPickStartDate)
        btnPickStartDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select start date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                selectedStartDateMs  = millis
                btnPickStartDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(millis)
            }
            picker.show(parentFragmentManager, "start_date_picker")
        }

        val btnPickDate = view.findViewById<MaterialButton>(R.id.btnPickDate)
        btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select due date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                selectedDateMs   = millis
                btnPickDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(millis)
            }
            picker.show(parentFragmentManager, "date_picker")
        }

        val btnRepeatDaily   = view.findViewById<MaterialButton>(R.id.btnRepeatDaily)
        val btnRepeatWeekly  = view.findViewById<MaterialButton>(R.id.btnRepeatWeekly)
        val btnRepeatMonthly = view.findViewById<MaterialButton>(R.id.btnRepeatMonthly)
        val btnRepeatCustom  = view.findViewById<MaterialButton>(R.id.btnRepeatCustom)
        val repeatBtns       = listOf(btnRepeatDaily, btnRepeatWeekly, btnRepeatMonthly, btnRepeatCustom)

        fun selectRepeat(chosen: MaterialButton, type: RepeatType) {
            repeatBtns.forEach { it.isChecked = it === chosen }
            selectedRepeat          = type
            layoutCustom.visibility = if (type == RepeatType.CUSTOM) View.VISIBLE else View.GONE
        }

        repeatBtns.forEach { it.isCheckable = true }
        selectRepeat(btnRepeatDaily, RepeatType.DAILY)
        btnRepeatDaily.setOnClickListener   { selectRepeat(btnRepeatDaily,   RepeatType.DAILY)   }
        btnRepeatWeekly.setOnClickListener  { selectRepeat(btnRepeatWeekly,  RepeatType.WEEKLY)  }
        btnRepeatMonthly.setOnClickListener { selectRepeat(btnRepeatMonthly, RepeatType.MONTHLY) }
        btnRepeatCustom.setOnClickListener  { selectRepeat(btnRepeatCustom,  RepeatType.CUSTOM)  }

        val actvUnit = view.findViewById<AutoCompleteTextView>(R.id.actvUnit)
        val units = listOf("days", "weeks", "months", "years")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        actvUnit.setAdapter(unitAdapter)
        actvUnit.setText("days", false)
        actvUnit.setOnItemClickListener { _, _, position, _ ->
            selectedUnit = units[position]
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val title = etTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                tilTitle.error = "Title is required"
                return@setOnClickListener
            }
            tilTitle.error = null

            val interval = etInterval.text?.toString()?.toIntOrNull()?.takeIf { it > 0 } ?: 1

            onSaveTask(
                Task(
                    title          = title,
                    category       = selectedCategory,
                    startDateMs    = selectedStartDateMs,
                    dueDateMs      = selectedDateMs,
                    isRepeat       = isRepeat,
                    repeatType     = selectedRepeat,
                    repeatInterval = interval,
                    repeatUnit     = selectedUnit
                )
            )
            dismiss()
        }
    }
}