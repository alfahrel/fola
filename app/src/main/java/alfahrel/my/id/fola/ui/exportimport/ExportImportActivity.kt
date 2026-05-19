package alfahrel.my.id.fola.ui.exportimport

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.data.model.RepeatType
import alfahrel.my.id.fola.data.model.Task
import alfahrel.my.id.fola.data.repository.AppDatabase
import alfahrel.my.id.fola.data.repository.AppRepository
import alfahrel.my.id.fola.util.BaseActivity
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportImportActivity : BaseActivity() {

    private lateinit var repository: AppRepository
    private var exportScope = "all"
    private var conflictStrategy = "merge"

    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> writeExportFile(uri) }
            }
        }

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> readImportFile(uri) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_export_import)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = AppRepository(AppDatabase.getInstance(this))

        setupExportScopeButtons()
        setupConflictButtons()
        setupExportButton()
        setupImportButton()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupExportScopeButtons() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.btnGroupExportScope)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            exportScope = when (checkedId) {
                R.id.btnExportActive    -> "active"
                R.id.btnExportCompleted -> "completed"
                else                    -> "all"
            }
        }
    }

    private fun setupConflictButtons() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.btnGroupConflict)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            conflictStrategy = when (checkedId) {
                R.id.btnConflictReplace -> "replace"
                else                    -> "merge"
            }
        }
    }

    private fun setupExportButton() {
        findViewById<MaterialButton>(R.id.btnExport).setOnClickListener {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "fola_backup_$dateStr.json")
            }
            createFileLauncher.launch(intent)
        }
    }

    private fun setupImportButton() {
        findViewById<MaterialButton>(R.id.btnImport).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            openFileLauncher.launch(intent)
        }
    }

    private fun writeExportFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(this@ExportImportActivity).taskDao()
                val tasks = when (exportScope) {
                    "active"    -> dao.getAllActiveTasksSync()
                    "completed" -> dao.getAllCompletedTasksSync()
                    else        -> dao.getAllTasksSync()
                }

                val jsonArray = JSONArray()
                tasks.forEach { jsonArray.put(it.toJson()) }

                val root = JSONObject().apply {
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("scope", exportScope)
                    put("tasks", jsonArray)
                }

                contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportImportActivity, "Exported ${tasks.size} task(s) successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportImportActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun readImportFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw Exception("Could not read file")

                val jsonArray = JSONObject(raw).getJSONArray("tasks")
                val tasks = (0 until jsonArray.length()).map { jsonArray.getJSONObject(it).toTask() }

                withContext(Dispatchers.Main) { confirmImport(tasks) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportImportActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmImport(tasks: List<Task>) {
        val strategyLabel = if (conflictStrategy == "replace") "replace all existing tasks" else "merge with existing tasks"
        MaterialAlertDialogBuilder(this)
            .setTitle("Import ${tasks.size} task(s)?")
            .setMessage("This will $strategyLabel. Continue?")
            .setPositiveButton("Import") { _, _ -> performImport(tasks) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performImport(tasks: List<Task>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(this@ExportImportActivity).taskDao()

                if (conflictStrategy == "replace") {
                    dao.getAllTasksSync().forEach { dao.delete(it) }
                }

                tasks.forEach { dao.insert(it.copy(id = 0)) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportImportActivity, "Imported ${tasks.size} task(s) successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportImportActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun Task.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("category", category)
        put("startDateMs", startDateMs ?: JSONObject.NULL)
        put("dueDateMs", dueDateMs ?: JSONObject.NULL)
        put("isCompleted", isCompleted)
        put("isRepeat", isRepeat)
        put("repeatType", repeatType.name)
        put("repeatInterval", repeatInterval)
        put("repeatUnit", repeatUnit)
        put("streak", streak)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toTask(): Task = Task(
        id             = 0,
        title          = getString("title"),
        category       = optString("category", "Task"),
        startDateMs    = if (isNull("startDateMs")) null else getLong("startDateMs"),
        dueDateMs      = if (isNull("dueDateMs")) null else getLong("dueDateMs"),
        isCompleted    = optBoolean("isCompleted", false),
        isRepeat       = optBoolean("isRepeat", false),
        repeatType     = RepeatType.valueOf(optString("repeatType", RepeatType.DAILY.name)),
        repeatInterval = optInt("repeatInterval", 1),
        repeatUnit     = optString("repeatUnit", "days"),
        streak         = optInt("streak", 0),
        createdAt      = optLong("createdAt", System.currentTimeMillis())
    )
}