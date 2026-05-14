package alfahrel.my.id.fola

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import alfahrel.my.id.fola.data.FolaDatabase
import alfahrel.my.id.fola.data.FolaRepository
import alfahrel.my.id.fola.data.model.Task
import alfahrel.my.id.fola.ui.TaskAdapter
import alfahrel.my.id.fola.ui.sheet.AddSheet
import alfahrel.my.id.fola.ui.sheet.EditSheet
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var repo: FolaRepository
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        repo        = FolaRepository(FolaDatabase.getInstance(this))
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setupToolbar()
        setupRecyclerView()
        setupCategoryFilter()
        setupDateFilter()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvTasks)
        taskAdapter  = TaskAdapter(
            onToggle    = { task, checked -> lifecycleScope.launch { repo.setTaskCompleted(task.id, checked) } },
            onLongClick = { _ -> },
            onDelete    = { task -> deleteWithUndo(task) },
            onEdit      = { task -> openEditSheet(task) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter       = taskAdapter
        }
        taskAdapter.attachSwipeToDelete(recyclerView)
    }

    private fun setupCategoryFilter() {
        val btnCatAll   = findViewById<MaterialButton>(R.id.btnCatAll)
        val btnCatTask  = findViewById<MaterialButton>(R.id.btnCatTask)
        val btnCatHabit = findViewById<MaterialButton>(R.id.btnCatHabit)
        val buttons     = listOf(btnCatAll, btnCatTask, btnCatHabit)
        val filterMap   = mapOf(btnCatAll to null, btnCatTask to "Task", btnCatHabit to "Habit")

        fun select(selected: MaterialButton) {
            buttons.forEach { it.isChecked = false }
            selected.isChecked = true
            taskAdapter.setFilter(filterMap[selected])
            updateEmptyState()
        }

        buttons.forEach { btn -> btn.setOnClickListener { select(btn) } }
        select(btnCatAll)
    }

    private fun setupDateFilter() {
        val dateFilterLayout = findViewById<TextInputLayout>(R.id.tilDateFilter)
        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.actvDateFilter)

        val dateFilters = listOf(
            "All" to null,
            "Today" to 0,
            "Tomorrow" to 1,
            "This Week" to 7
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            dateFilters.map { it.first }
        )
        autoCompleteTextView.setAdapter(adapter)

        autoCompleteTextView.setText("All", false)

        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedFilter = dateFilters[position]
            taskAdapter.setDateFilter(selectedFilter.second)
            updateEmptyState()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repo.activeTasks.collectLatest {
                taskAdapter.submitActive(it)
                updateEmptyState()
            }
        }
        lifecycleScope.launch {
            repo.completedTasks.collectLatest {
                taskAdapter.submitCompleted(it)
                updateEmptyState()
            }
        }
    }

    private fun updateEmptyState() {
        layoutEmpty.visibility = if (taskAdapter.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteWithUndo(task: Task) {
        lifecycleScope.launch { repo.deleteTask(task) }
        Snackbar.make(findViewById(R.id.main), "Task deleted", Snackbar.LENGTH_LONG)
            .setAction("Undo") { lifecycleScope.launch { repo.insertTask(task) } }
            .setAnchorView(findViewById(R.id.fabAdd))
            .show()
    }

    private fun openEditSheet(task: Task) {
        EditSheet(
            task         = task,
            onUpdateTask = { updated -> lifecycleScope.launch { repo.updateTask(updated) } }
        ).show(supportFragmentManager, "EditSheet")
    }

    private fun setupFab() {
        findViewById<ExtendedFloatingActionButton>(R.id.fabAdd).setOnClickListener {
            AddSheet(
                onSaveTask = { task -> lifecycleScope.launch { repo.insertTask(task) } }
            ).show(supportFragmentManager, "AddSheet")
        }
    }
}