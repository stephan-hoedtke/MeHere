package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class Repository {

    enum class MyPositionSource {
        SCROLLING,
        MODEL,
    }

    internal class MyPosition(val currentLocation: Position, val center: Position, val source: MyPositionSource) {

        internal val isSomewhereElse
            get() = currentLocation.isSomewhereElse(center)

        internal fun isSomewhereElse(newLocation: Position, newCenter: Position): Boolean =
            currentLocation.isSomewhereElse(newLocation) || center.isSomewhereElse(newCenter)
    }

    private val positionLiveData = MutableLiveData<MyPosition>()
    private val zoomLiveData = MutableLiveData<Double>()

    init {
        positionLiveData.value = MyPosition(defaultLocationBerlinBuch, defaultLocationBerlinBuch, MyPositionSource.MODEL)
        zoomLiveData.value = defaultZoom
    }

    internal val positionLD: LiveData<MyPosition>
        get() = positionLiveData

    internal val currentLocation: Position
        get() = positionLiveData.value!!.currentLocation

    internal val center: Position
        get() = positionLiveData.value!!.center

    internal fun setCurrentLocationMoveCenter(newCurrentLocation: Position, source: MyPositionSource) =
        setLocation(newCurrentLocation, newCurrentLocation, source)

    internal fun setCurrentLocationKeepCenter(newCurrentLocation: Position, source: MyPositionSource) =
        setLocation(newCurrentLocation, center, source)

    internal fun setCenterKeepCurrentLocation(newCenter: Position, source: MyPositionSource) =
        setLocation(currentLocation, newCenter, source)

    private fun setLocation(newCurrentLocation: Position, newCenter: Position, source: MyPositionSource) {
        if (positionLiveData.value!!.isSomewhereElse(newCurrentLocation, newCenter)) {
            positionLiveData.postValue(MyPosition(newCurrentLocation, newCenter, source))
        }
    }

    internal val zoomLD: LiveData<Double>
        get() = zoomLiveData

    internal var zoom: Double
        get() = zoomLiveData.value!!
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
            positionLiveData.value = MyPosition(currentLocation, center = Position(latitude, longitude, altitude), source = MyPositionSource.MODEL)
            zoomLiveData.value = zoomLevel
        }
    }

    internal fun save(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        currentLocation.also {
            editor.putDouble(centerLatitudeKey, it.latitude)
            editor.putDouble(centerLongitudeKey, it.longitude)
            editor.putDouble(centerAltitudeKey, it.altitude)
        }
        zoom.also {
            editor.putDouble(zoomKey, it)
        }
        editor.apply()
    }

    internal fun touch() {
        positionLiveData.postValue(MyPosition(currentLocation, center, MyPositionSource.MODEL))
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

private fun SharedPreferences.getDouble(key: String, defaultValue: Double): Double =
    this.getFloat(key, defaultValue.toFloat()).toDouble()

private fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
    this.putFloat(key, value.toFloat())
}
