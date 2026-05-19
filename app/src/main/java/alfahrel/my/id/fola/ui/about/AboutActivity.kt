package alfahrel.my.id.fola.ui.about

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.util.BaseActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.tvVersion).text = "Version 0.1.3-alpha"

        findViewById<MaterialButton>(R.id.btnSourceCode).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alfahrel/fola")))
        }

        findViewById<MaterialButton>(R.id.btnViewLicense).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html")))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}