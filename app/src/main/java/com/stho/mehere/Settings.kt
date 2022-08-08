package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class Settings(context: Context) {

    internal var useOrientation: Boolean = true
        private set

    internal var useLocation: Boolean = true
        private set

    internal var useTracking: Boolean = true
        private set

    internal var rotateMapView: Boolean = false
        private set

    internal var useMapsForge: Boolean = true
        private set

    internal var scaleFonts: Boolean = false
        private set

    internal var home: Location = Repository.defaultLocationBerlinBuch

    internal var alpha: Float = 0.5f

    init {
        load(context)
    }

    internal fun load(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).let {
            useTracking = it.getBoolean(trackingKey, true)
            useLocation = it.getBoolean(locationKey, true)
            useOrientation = it.getBoolean(orientationKey, true)
            rotateMapView = it.getBoolean(rotateMapViewKey, true)
            useMapsForge = it.getBoolean(useMapsForgeKey, true)
            scaleFonts = it.getBoolean(scaleFontsKey, true)
            home = Location(
                latitude = it.getStringAsDouble(homeLatitudeKey, home.latitude),
                longitude = it.getStringAsDouble(homeLongitudeKey, home.longitude),
                altitude = home.altitude
            )
            alpha = it.getFloat(alphaKey, 0.5f)
        }
    }

    internal fun save(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        editor.putBoolean(trackingKey, useTracking)
        editor.putBoolean(locationKey, useLocation)
        editor.putBoolean(orientationKey, useOrientation)
        editor.putBoolean(rotateMapViewKey, rotateMapView)
        editor.putBoolean(useMapsForgeKey, useMapsForge)
        editor.putBoolean(scaleFontsKey, scaleFonts)
        editor.putString(homeLatitudeKey, home.latitude.toString())
        editor.putString(homeLongitudeKey, home.longitude.toString())
        editor.putFloat(alphaKey, alpha)
        editor.apply()
    }

    companion object {
        // Mind: these keys are the same keys as in preferences.xml
        // TODO: make it capital
        private const val trackingKey = "tracking"
        private const val locationKey = "location"
        private const val orientationKey = "orientation"
        private const val rotateMapViewKey = "rotateMapView"
        private const val useMapsForgeKey = "useMapsForge"
        private const val scaleFontsKey = "scaleFonts"
        private const val homeLatitudeKey = "homeLatitude"
        private const val homeLongitudeKey = "homeLongitude"
        private const val alphaKey = "alpha"
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
