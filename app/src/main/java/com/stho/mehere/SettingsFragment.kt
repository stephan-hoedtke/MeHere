package com.stho.mehere

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        updateActionBar(resources.getString(R.string.action_settings))
    }

    private fun updateActionBar(title: String) {
        supportActionBar?.apply {
            this.title = title
            this.subtitle = null
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private val supportActionBar: androidx.appcompat.app.ActionBar?
        get() = (activity as AppCompatActivity?)?.supportActionBar

}