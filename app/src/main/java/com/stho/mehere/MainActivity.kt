package com.stho.mehere

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.mapsforge.MapsForgeTileSource
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private lateinit var repository: Repository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        handler = Handler(Looper.getMainLooper())
        repository = Repository.requireRepository(this)
        repository.isDirtyLD.observe(this) { isDirty -> observeIsDirty(isDirty) }

        // MapsForge:
        MapsForgeTileSource.createInstance(this.application);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun observeIsDirty(isDirty: Boolean) {
        if (isDirty) {
            handler.postDelayed({
                CoroutineScope(Dispatchers.Default).launch {
                    if (repository.isDirty)
                        repository.save(this@MainActivity)
                }
            }, 200)
        }
    }

    override fun onPause() {
        super.onPause()
        stopHandler()
    }

    private fun stopHandler() =
        handler.removeCallbacksAndMessages(null)
}

internal fun FragmentActivity.showError(exception: Exception) {
    val container: View = findViewById<View>(R.id.nav_host_fragment)
    showError(container, exception)
}

internal fun FragmentActivity.showError(container: View, exception: Exception) {
    val message = exception.message ?: exception.toString()
    Snackbar.make(container, message, Snackbar.LENGTH_LONG).also {
        it.setAction(R.string.label_close) { _ -> it.dismiss() }
        it.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4
        it.setBackgroundTint(getColor(R.color.my_app_error_container))
        it.setTextColor(getColor(R.color.my_app_on_error_container))
        it.setActionTextColor(getColor(R.color.my_app_on_error_container))
        it.setDuration(23000)
        it.show()
    }
}

internal fun FragmentActivity.showMessage(container: View, message: String) {
    Snackbar.make(container, message, Snackbar.LENGTH_LONG).also {
        it.setAction(R.string.label_close) { _ -> it.dismiss() }
        it.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4
        it.setBackgroundTint(getColor(R.color.my_app_primary_inverse))
        it.setTextColor(getColor(R.color.my_app_on_primary_container))
        it.setActionTextColor(getColor(R.color.my_app_on_primary_container))
        it.setDuration(23000)
        it.show()
    }
}
