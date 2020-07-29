package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.osmdroid.util.GeoPoint

class Repository {

    private val currentLocationLiveData = MutableLiveData<Position>()
    private val centerLiveData = MutableLiveData<Position>()
    private val zoomLiveData = MutableLiveData<Double>()

    init {
        currentLocationLiveData.value = defaultLocationBerlinBuch
        centerLiveData.value = defaultLocationBerlinBuch
        zoomLiveData.value = defaultZoom
    }

    internal val currentLocationLD: LiveData<Position>
        get() = currentLocationLiveData

    internal var currentLocation: Position
        get() = currentLocationLiveData.value ?: defaultLocationBerlinBuch
        set(value) {
            currentLocationLiveData.postValue(value)
        }

    internal val centerLD: LiveData<Position>
        get() = centerLiveData

    internal var center: Position
        get() = centerLiveData.value ?: defaultLocationBerlinBuch
        set(value) {
            centerLiveData.postValue(value)
        }

    internal val zoomLD: LiveData<Double>
        get() = zoomLiveData

    internal var zoom: Double
        get() = zoomLiveData.value ?: defaultZoom
        set(value) {
            if (zoomLiveData.value != value) {
                zoomLiveData.postValue(value)
            }
        }

    private fun load(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).also {
            val latitude = it.getDouble(centerLatitudeKey, defaultLatitude)
            val longitude =  it.getDouble(centerLongitudeKey, defaultLongitude)
            val altitude = it.getDouble(centerAltitudeKey, defaultAltitude)
            val zoomLevel =  it.getDouble(zoomKey, defaultZoom)
            centerLiveData.value = Position(latitude, longitude, altitude)
            zoomLiveData.value = zoomLevel
        }
    }

    internal fun save(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        centerLiveData.value?.also {
            editor.putDouble(centerLatitudeKey, it.latitude)
            editor.putDouble(centerLongitudeKey, it.longitude)
            editor.putDouble(centerAltitudeKey, it.altitude)
        }
        zoomLiveData.value?.also {
            editor.putDouble(zoomKey, it)
        }
        editor.apply()
    }

    companion object {

        private const val centerLatitudeKey = "center.latitude"
        private const val centerLongitudeKey = "center.longitude"
        private const val centerAltitudeKey = "center.altitude"
        private const val zoomKey = "zoom"

        internal const val defaultLatitude = 52.64511
        internal const val defaultLongitude = 13.49181
        internal const val defaultAltitude = 90.0
        internal const val defaultZoom = 13.0

        internal val defaultLocationBerlinBuch: Position by lazy {
            Position(defaultLatitude, defaultLongitude, defaultAltitude)
        }

        private var singleton: Repository? = null
        private var lockObject: Any = Any()

        fun requireRepository(context: Context): Repository {

            if (singleton != null) {
                return singleton!!
            }

            synchronized(lockObject) {
                if (singleton == null) {
                    singleton = Repository().also {
                        it.load(context)
                    }
                }
                return singleton!!
            }
        }
    }
}

// Extensions used above:

private fun SharedPreferences.getDouble(key: String, defaultValue: Double): Double {
    return this.getFloat(key, defaultValue.toFloat()).toDouble()
}

private fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
    this.putFloat(key, value.toFloat())
}
