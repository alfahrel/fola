package alfahrel.my.id.fola.ui.language

import alfahrel.my.id.fola.R
import alfahrel.my.id.fola.util.BaseActivity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar

class LanguageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}