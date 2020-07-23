package com.stho.mehere

import android.content.Context
import androidx.preference.PreferenceManager

class Settings(context: Context) {

    internal var useOrientation: Boolean = true
        private set

    internal var useLocation: Boolean = true
        private set

    internal var useTracking: Boolean = true
        private set

    init {
        update(context)
    }

    internal fun update(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).let {
            this.useTracking = it.getBoolean("tracking", true)
            this.useLocation = it.getBoolean("location", true)
            this.useOrientation = it.getBoolean("orientation", true)
        }
    }
}
