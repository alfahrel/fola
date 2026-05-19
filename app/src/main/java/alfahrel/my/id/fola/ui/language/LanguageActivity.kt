package alfahrel.my.id.fola.ui.language

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.util.BaseActivity
import alfahrel.my.id.fola.util.LanguagePrefs
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LanguageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupLanguageButtons()
    }

    private fun setupLanguageButtons() {
        val group      = findViewById<MaterialButtonToggleGroup>(R.id.btnGroupLanguage)
        val btnEnglish = findViewById<MaterialButton>(R.id.btnLangEnglish)
        val btnIndo    = findViewById<MaterialButton>(R.id.btnLangIndonesian)

        val current = LanguagePrefs.loadLanguage(this)
        group.check(if (current == "in") btnIndo.id else btnEnglish.id)

        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val langCode = if (checkedId == R.id.btnLangIndonesian) "in" else "en"
            if (langCode == LanguagePrefs.loadLanguage(this)) return@addOnButtonCheckedListener

            MaterialAlertDialogBuilder(this)
                .setTitle("Change language")
                .setMessage("The app will restart to apply the new language. Continue?")
                .setPositiveButton("Restart") { _, _ ->
                    LanguagePrefs.saveLanguage(this, langCode)
                    restartApp()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    val previous = LanguagePrefs.loadLanguage(this)
                    group.check(if (previous == "in") btnIndo.id else btnEnglish.id)
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