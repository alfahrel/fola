package alfahrel.my.id.fola.ui.theme

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.ThemePrefs
import alfahrel.my.id.fola.util.App
import alfahrel.my.id.fola.util.BaseActivity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class ThemeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_theme)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupModeButtons()
        setupDynamicColorSwitch()
    }

    private fun setupModeButtons() {
        val btnGroup = findViewById<MaterialButtonToggleGroup>(R.id.btnGroupMode)
        val btnSystem = findViewById<MaterialButton>(R.id.btnSystem)
        val btnLight = findViewById<MaterialButton>(R.id.btnLight)
        val btnDark = findViewById<MaterialButton>(R.id.btnDark)

        val currentMode = ThemePrefs.getMode(this)
        val initialButton = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> btnLight
            AppCompatDelegate.MODE_NIGHT_YES -> btnDark
            else -> btnSystem
        }
        btnGroup.check(initialButton.id)

        btnGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val mode = when (checkedId) {
                R.id.btnLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.btnDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            ThemePrefs.setMode(this, mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun setupDynamicColorSwitch() {
        val switch = findViewById<MaterialSwitch>(R.id.switchDynamicColor)
        switch.isChecked = ThemePrefs.isDynamicColor(this)

        switch.setOnCheckedChangeListener { _, isChecked ->
            MaterialAlertDialogBuilder(this)
                .setTitle(if (isChecked) "Enable Dynamic Color" else "Disable Dynamic Color")
                .setMessage("The app will restart to apply the new color theme. Continue?")
                .setPositiveButton("Restart") { _, _ ->
                    ThemePrefs.setDynamicColor(this, isChecked)
                    (application as App).applyDynamicColors()
                    restartApp()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    switch.setOnCheckedChangeListener(null)
                    switch.isChecked = !isChecked
                    setupDynamicColorSwitch()
                }
                .show()
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}