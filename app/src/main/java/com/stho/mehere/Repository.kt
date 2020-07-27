package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.osmdroid.util.GeoPoint

class Repository {

    private val currentLocationLiveData = MutableLiveData<Location>()
    private val centerLiveData = MutableLiveData<GeoPoint>()
    private val zoomLevelLiveData = MutableLiveData<Double>()

    init {
        currentLocationLiveData.value = defaultLocationBerlinBuch
        centerLiveData.value = GeoPoint(defaultLocationBerlinBuch)
        zoomLevelLiveData.value = defaultZoomLevel
    }

    internal val currentLocationLD: LiveData<Location>
        get() = currentLocationLiveData

    internal var currentLocation: Location
        get() = currentLocationLiveData.value ?: defaultLocationBerlinBuch
        set(value) {
            currentLocationLiveData.postValue(value)
        }

    internal val centerLD: LiveData<GeoPoint>
        get() = centerLiveData

    internal var center: GeoPoint
        get() = centerLiveData.value ?: GeoPoint(defaultLocationBerlinBuch)
        set(value) {
            centerLiveData.postValue(value)
        }

    internal val zoomLevelLD: LiveData<Double>
        get() = zoomLevelLiveData

    internal var zoomLevel: Double
        get() = zoomLevelLiveData.value ?: defaultZoomLevel
        set(value) {
            if (zoomLevelLiveData.value != value) {
                zoomLevelLiveData.postValue(value)
            }
        }

    private fun load(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).also {
            val latitude = it.getDouble(centerLatitudeKey, defaultLatitude)
            val longitude =  it.getDouble(centerLongitudeKey, defaultLongitude)
            val altitude = it.getDouble(centerAltitudeKey, defaultAltitude)
            val zoomLevel =  it.getDouble(zoomLevelKey, defaultZoomLevel)
            centerLiveData.value = GeoPoint(latitude, longitude, altitude)
            zoomLevelLiveData.value = zoomLevel
        }
    }

    internal fun save(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        centerLiveData.value?.also {
            editor.putDouble(centerLatitudeKey, it.latitude)
            editor.putDouble(centerLongitudeKey, it.longitude)
            editor.putDouble(centerAltitudeKey, it.altitude)
        }
        zoomLevelLiveData.value?.also {
            editor.putDouble(zoomLevelKey, it)
        }
        editor.apply()
    }

    companion object {

        private const val centerLatitudeKey = "center.latitude"
        private const val centerLongitudeKey = "center.longitude"
        private const val centerAltitudeKey = "center.altitude"
        private const val zoomLevelKey = "zoomLevel"

        private const val defaultLatitude = 52.64511
        private const val defaultLongitude = 13.49181
        private const val defaultAltitude = 90.0

        internal const val defaultZoomLevel = 13.0

        internal val defaultLocationBerlinBuch: Location
            get() = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = defaultLatitude
                longitude = defaultLongitude
                altitude = defaultAltitude // in m above see  level
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
