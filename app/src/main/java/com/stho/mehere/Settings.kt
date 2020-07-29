package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.preference.PreferenceManager

class Settings(context: Context) {

    internal var useOrientation: Boolean = true
        private set

    internal var useLocation: Boolean = true
        private set

    internal var useTracking: Boolean = true
        private set

    internal var home: Position = Repository.defaultLocationBerlinBuch

    init {
        load(context)
    }

    internal fun load(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).let {
            useTracking = it.getBoolean(trackingKey, true)
            useLocation = it.getBoolean(locationKey, true)
            useOrientation = it.getBoolean(orientationKey, true)
            home = Position(
                latitude = it.getStringAsDouble(homeLatitudeKey, home.latitude),
                longitude = it.getStringAsDouble(homeLongitudeKey, home.longitude),
                altitude = home.altitude
            )
        }
    }

    internal fun save(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        editor.putBoolean(trackingKey, useTracking)
        editor.putBoolean(locationKey, useLocation)
        editor.putBoolean(orientationKey, useOrientation)
        editor.putString(homeLatitudeKey, home.latitude.toString())
        editor.putString(homeLongitudeKey, home.longitude.toString())
        editor.apply()
    }

    companion object {
        // Mind: these keys are the same keys as in preferences.xml
        private const val trackingKey = "tracking"
        private const val locationKey = "location"
        private const val orientationKey = "orientation"
        private const val homeLatitudeKey = "homeLatitude"
        private const val homeLongitudeKey = "homeLongitude"
    }


}

private fun SharedPreferences.getStringAsDouble(key: String, defaultValue: Double): Double {
    val s = this.getString(key, null)
    if (s != null) {
        val d = s.toDoubleOrNull()
        if (d != null) {
            return d
        }
    }
    return defaultValue
}
