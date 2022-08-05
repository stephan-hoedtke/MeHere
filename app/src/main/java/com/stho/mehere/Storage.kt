package com.stho.mehere;

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.lang.Exception


class Storage(private val context: Context) {

    private val preferences: SharedPreferences
        = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun writeCenter(center: Location) {
        val json = Json.encodeToString(Location.serializer(), center)
        preferences.edit().also {
            it.putString(CENTER_KEY, json)
            it.apply()
        }
    }

    fun readCenter(defaultValue: Location): Location {
        return preferences.getString(CENTER_KEY, null)?.let {
            Json.decodeFromString(Location.serializer(), it)
        } ?: defaultValue
    }

    fun writeZoom(zoom: Double) {
        preferences.edit().also {
            it.putDouble(ZOOM_KEY, zoom)
            it.apply()
        }
    }

    fun readZoom(defaultValue: Double): Double {
        return preferences.getDouble(ZOOM_KEY, defaultValue)
    }

    fun writePoints(points: List<PointOfInterest>) {
        val json = Json.encodeToString(ListSerializer(PointOfInterest.serializer()), points)
        preferences.edit().also {
            it.putString(POINTS_KEY, json)
            it.apply()
        }

    }

    fun readPoints(): MutableList<PointOfInterest> {
        val json = preferences.getString(POINTS_KEY, null)
        return json?.let { decodePoints(it) } ?: mutableListOf()
    }

    private fun decodePoints(json: String): MutableList<PointOfInterest>? =
        try {
            Json.decodeFromString(ListSerializer(PointOfInterest.serializer()), json).toMutableList()
        } catch (ex: Exception) {
            null
        }

    companion object {
        private const val CENTER_KEY = "center"
        private const val ZOOM_KEY = "zoom"
        private const val POINTS_KEY = "points";
    }
}


// Extensions used above:

private fun SharedPreferences.getDouble(key: String, defaultValue: Double): Double =
    this.getFloat(key, defaultValue.toFloat()).toDouble()

private fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
    this.putFloat(key, value.toFloat())
}
