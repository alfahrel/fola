package alfahrel.my.id.fola

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import alfahrel.my.id.fola.widget.FolaWidgetProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    companion object {
        const val EXTRA_OPEN_ADD_SHEET = "open_add_sheet"
    }

    private lateinit var repo: FolaRepository
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        repo = FolaRepository(FolaDatabase.getInstance(this))
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setupToolbar()
        setupRecyclerView()
        setupCategoryFilter()
        setupDateFilter()
        setupFab()
        observeData()

        if (intent?.getBooleanExtra(EXTRA_OPEN_ADD_SHEET, false) == true) {
            openAddSheet()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD_SHEET, false)) {
            openAddSheet()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_calendar -> {
                startActivity(Intent(this, CalendarActivity::class.java))
                true
            }
            R.id.action_theme -> {
                startActivity(Intent(this, ThemeActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvTasks)
        taskAdapter = TaskAdapter(
            onToggle = { task, checked ->
                lifecycleScope.launch {
                    repo.setTaskCompleted(task.id, checked)
                    notifyWidget()
                }
            },
            onLongClick = { _ -> },
            onDelete = { task -> deleteWithUndo(task) },
            onEdit = { task -> openEditSheet(task) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
        taskAdapter.attachSwipeToDelete(recyclerView)
    }

    private fun setupCategoryFilter() {
        val categoryGroup = findViewById<MaterialButtonToggleGroup>(R.id.categoryGroup)
        val btnCatAll = findViewById<MaterialButton>(R.id.btnCatAll)
        val btnCatTask = findViewById<MaterialButton>(R.id.btnCatTask)
        val btnCatHabit = findViewById<MaterialButton>(R.id.btnCatHabit)

        val filterMap = mapOf(
            R.id.btnCatAll to null,
            R.id.btnCatTask to "Task",
            R.id.btnCatHabit to "Habit"
        )

        categoryGroup.check(R.id.btnCatAll)
        categoryGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            taskAdapter.setFilter(filterMap[checkedId])
            updateEmptyState()
        }
    }

    private fun setupDateFilter() {
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
        taskAdapter.setDateFilter(null)

        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedFilter = dateFilters[position].second
            taskAdapter.setDateFilter(selectedFilter)
            updateEmptyState()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repo.activeTasks.collectLatest {
                taskAdapter.submitActive(it)
                updateEmptyState()
                notifyWidget()
            }
        }
        lifecycleScope.launch {
            repo.completedTasks.collectLatest {
                taskAdapter.submitCompleted(it)
                updateEmptyState()
                notifyWidget()
            }
        }
    }

    private fun updateEmptyState() {
        layoutEmpty.visibility = if (taskAdapter.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteWithUndo(task: Task) {
        lifecycleScope.launch { repo.deleteTask(task) }
        Snackbar.make(findViewById(R.id.main), "Task deleted", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                lifecycleScope.launch {
                    repo.insertTask(task)
                    notifyWidget()
                }
            }
            .setAnchorView(findViewById(R.id.fabAdd))
            .show()
    }

    private fun openEditSheet(task: Task) {
        EditSheet(
            task = task,
            onUpdateTask = { updated ->
                lifecycleScope.launch {
                    repo.updateTask(updated)
                    notifyWidget()
                }
            }
        ).show(supportFragmentManager, "EditSheet")
    }

    private fun openAddSheet() {
        AddSheet(
            onSaveTask = { task ->
                lifecycleScope.launch {
                    repo.insertTask(task)
                    notifyWidget()
                }
            }
        ).show(supportFragmentManager, "AddSheet")
    }

    private fun setupFab() {
        findViewById<ExtendedFloatingActionButton>(R.id.fabAdd).setOnClickListener {
            openAddSheet()
        }
    }

    private fun notifyWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, FolaWidgetProvider::class.java))
        ids.forEach { FolaWidgetProvider.updateWidget(this, manager, it) }
    }
}