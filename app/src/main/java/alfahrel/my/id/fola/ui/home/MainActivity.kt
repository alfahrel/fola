package alfahrel.my.id.fola.ui.home

import alfahrel.my.id.fola.util.BaseActivity
import alfahrel.my.id.fola.ui.calendar.CalendarActivity
import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.ui.theme.ThemeActivity
import alfahrel.my.id.fola.data.model.Task
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.data.repository.AppRepository
import alfahrel.my.id.fola.service.MidnightWorker
import alfahrel.my.id.fola.ui.about.AboutActivity
import alfahrel.my.id.fola.ui.exportimport.ExportImportActivity
import alfahrel.my.id.fola.ui.language.LanguageActivity
import alfahrel.my.id.fola.ui.task.TaskAdapter
import alfahrel.my.id.fola.ui.sheet.AddSheet
import alfahrel.my.id.fola.ui.sheet.EditSheet
import alfahrel.my.id.fola.util.FilterPrefs
import alfahrel.my.id.fola.widget.TaskWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    companion object {
        const val EXTRA_OPEN_ADD_SHEET = "open_add_sheet"
    }

    private lateinit var repo: AppRepository
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        repo = AppRepository(AppDatabase.getInstance(this))
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setupToolbar()
        setupRecyclerView()
        setupDateFilter()
        setupCategoryFilter()
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
            R.id.action_export_import -> {
                startActivity(Intent(this, ExportImportActivity::class.java))
                true
            }
            R.id.action_language -> {
                startActivity(Intent(this, LanguageActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
//            R.id.action_debug_midnight -> {
//                triggerMidnightWorker()
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun triggerMidnightWorker() {
        val request = OneTimeWorkRequestBuilder<MidnightWorker>().build()
        WorkManager.getInstance(this).enqueue(request)
        Toast.makeText(this, "Midnight worker triggered", Toast.LENGTH_SHORT).show()
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

    private fun setupDateFilter() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.dateGroup)

        val savedDate = FilterPrefs.loadDateFilter(this)
        val dateButtonId = when (savedDate) {
            0    -> R.id.btnDateToday
            1    -> R.id.btnDateTomorrow
            7    -> R.id.btnDateWeek
            else -> R.id.btnDateAll
        }
        group.check(dateButtonId)
        taskAdapter.setDateFilter(savedDate)

        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val daysAhead = when (checkedId) {
                R.id.btnDateToday    -> 0
                R.id.btnDateTomorrow -> 1
                R.id.btnDateWeek     -> 7
                else                 -> null
            }
            FilterPrefs.saveDateFilter(this, daysAhead)
            taskAdapter.setDateFilter(daysAhead)
            updateEmptyState()
        }
    }

    private fun setupCategoryFilter() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.categoryGroup)

        val savedCategory = FilterPrefs.loadCategoryFilter(this)
        val catButtonId = when (savedCategory) {
            "Task"  -> R.id.btnCatTask
            "Habit" -> R.id.btnCatHabit
            else    -> R.id.btnCatAll
        }
        group.check(catButtonId)
        taskAdapter.setFilter(savedCategory)

        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val category = when (checkedId) {
                R.id.btnCatTask  -> "Task"
                R.id.btnCatHabit -> "Habit"
                else             -> null
            }
            FilterPrefs.saveCategoryFilter(this, category)
            taskAdapter.setFilter(category)
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
        val ids = manager.getAppWidgetIds(ComponentName(this, TaskWidgetProvider::class.java))
        ids.forEach { TaskWidgetProvider.updateWidget(this, manager, it) }
    }
}