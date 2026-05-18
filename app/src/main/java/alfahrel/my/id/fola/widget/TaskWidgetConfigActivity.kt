package alfahrel.my.id.fola.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import alfahrel.my.id.fola.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class TaskWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val dateOptions = listOf(
        "All"       to null,
        "Today"     to 0,
        "Tomorrow"  to 1,
        "This Week" to 7
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent
            ?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        setContentView(R.layout.widget_task_config)

        setSupportActionBar(findViewById<Toolbar>(R.id.configToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val actv             = findViewById<AutoCompleteTextView>(R.id.actvConfigDateFilter)
        val switchCompleted  = findViewById<MaterialSwitch>(R.id.switchShowCompleted)
        val switchCategory   = findViewById<MaterialSwitch>(R.id.switchShowCategory)
        val switchDate       = findViewById<MaterialSwitch>(R.id.switchShowDate)
        val btnSave          = findViewById<MaterialButton>(R.id.btnConfigSave)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            dateOptions.map { it.first }
        )
        actv.setAdapter(adapter)

        val savedFilter = WidgetPrefs.loadDateFilter(this, appWidgetId)
        actv.setText(dateOptions.find { it.second == savedFilter }?.first ?: "All", false)
        switchCompleted.isChecked = WidgetPrefs.loadShowCompleted(this, appWidgetId)
        switchCategory.isChecked  = WidgetPrefs.loadShowCategory(this, appWidgetId)
        switchDate.isChecked      = WidgetPrefs.loadShowDate(this, appWidgetId)

        btnSave.setOnClickListener {
            val selectedFilter = dateOptions.find { it.first == actv.text.toString() }?.second
            WidgetPrefs.saveDateFilter(this, appWidgetId, selectedFilter)
            WidgetPrefs.saveShowCompleted(this, appWidgetId, switchCompleted.isChecked)
            WidgetPrefs.saveShowCategory(this, appWidgetId, switchCategory.isChecked)
            WidgetPrefs.saveShowDate(this, appWidgetId, switchDate.isChecked)

            TaskWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), appWidgetId)

            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}