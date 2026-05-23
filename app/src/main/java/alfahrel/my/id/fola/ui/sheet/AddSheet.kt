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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.model.RepeatType
import alfahrel.my.id.fola.data.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddSheet(
    private val onSaveTask: (Task) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedDueDateMs:        Long?       = null
    private var selectedCategory:         String      = "Task"
    private var isRepeat:                 Boolean     = false
    private var selectedRepeat:           RepeatType  = RepeatType.DAILY
    private var selectedUnit:             String      = "days"
    private var selectedStartDateMs:      Long?       = null
    private var selectedHabitStartDateMs: Long?       = null
    private var selectedHabitEndDateMs:   Long?       = null

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

        val categoryGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.categoryGroup)
        categoryGroup.check(R.id.btnCatTask)
        categoryGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnCatTask -> {
                    selectedCategory             = "Task"
                    layoutTaskOptions.visibility = View.VISIBLE
                    layoutHabit.visibility       = View.GONE
                    isRepeat                     = false
                }
                R.id.btnCatHabit -> {
                    selectedCategory             = "Habit"
                    layoutTaskOptions.visibility = View.GONE
                    layoutHabit.visibility       = View.VISIBLE
                    isRepeat                     = true
                }
            }
        }

        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun utcPickerToLocalMidnight(millis: Long): Long {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = millis
            }
            return Calendar.getInstance().apply {
                set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val btnPickStartDate = view.findViewById<MaterialButton>(R.id.btnPickStartDate)
        btnPickStartDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select start date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                selectedStartDateMs   = utcPickerToLocalMidnight(millis)
                btnPickStartDate.text = fmt.format(selectedStartDateMs!!)
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
                selectedDueDateMs = utcPickerToLocalMidnight(millis)
                btnPickDate.text  = fmt.format(selectedDueDateMs!!)
            }
            picker.show(parentFragmentManager, "date_picker")
        }

        val btnPickHabitStartDate = view.findViewById<MaterialButton>(R.id.btnPickHabitStartDate)
        btnPickHabitStartDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select habit start date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                selectedHabitStartDateMs   = utcPickerToLocalMidnight(millis)
                btnPickHabitStartDate.text = fmt.format(selectedHabitStartDateMs!!)
            }
            picker.show(parentFragmentManager, "habit_start_date_picker")
        }

        val btnPickHabitEndDate = view.findViewById<MaterialButton>(R.id.btnPickHabitEndDate)
        btnPickHabitEndDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select habit end date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                selectedHabitEndDateMs   = utcPickerToLocalMidnight(millis)
                btnPickHabitEndDate.text = fmt.format(selectedHabitEndDateMs!!)
            }
            picker.show(parentFragmentManager, "habit_end_date_picker")
        }

        val repeatGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.repeatGroup)
        repeatGroup.check(R.id.btnRepeatDaily)
        repeatGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedRepeat = when (checkedId) {
                R.id.btnRepeatDaily   -> RepeatType.DAILY
                R.id.btnRepeatWeekly  -> RepeatType.WEEKLY
                R.id.btnRepeatMonthly -> RepeatType.MONTHLY
                else                  -> RepeatType.CUSTOM
            }
            layoutCustom.visibility = if (selectedRepeat == RepeatType.CUSTOM) View.VISIBLE else View.GONE
        }

        val actvUnit    = view.findViewById<AutoCompleteTextView>(R.id.actvUnit)
        val units       = listOf("days", "weeks", "months", "years")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        actvUnit.setAdapter(unitAdapter)
        actvUnit.setText("days", false)
        actvUnit.setOnItemClickListener { _, _, position, _ -> selectedUnit = units[position] }

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
                    startDateMs    = if (selectedCategory == "Habit") selectedHabitStartDateMs else selectedStartDateMs,
                    dueDateMs      = if (selectedCategory == "Habit") selectedHabitEndDateMs   else selectedDueDateMs,
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