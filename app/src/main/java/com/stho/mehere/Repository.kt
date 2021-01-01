package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint

class Repository {

    internal class Center(val center: Position, val source: Source) {

        enum class Source {
            SCROLLING,
            MODEL,
        }

        internal fun isSomewhereElse(point: IGeoPoint): Boolean =
            center.isSomewhereElse(point)

        internal fun toGeoPoint(): GeoPoint =
            center.toGeoPoint()
    }

    private val currentPositionLiveData = MutableLiveData<Position>()
    private val centerLiveData = MutableLiveData<Center>()
    private val zoomLiveData = MutableLiveData<Double>()

    init {
        currentPositionLiveData.value = defaultLocationBerlinBuch
        centerLiveData.postValue(Center(defaultLocationBerlinBuch, Center.Source.MODEL))
        zoomLiveData.value = defaultZoom
    }

    internal val currentPositionLD: LiveData<Position>
        get() = currentPositionLiveData

    internal val currentPosition: Position
        get() = currentPositionLiveData.value ?: defaultLocationBerlinBuch

    internal val centerLD: LiveData<Center>
        get() = centerLiveData

    internal val center: Position
        get() = centerLiveData.value!!.center

    internal fun setCurrentLocationMoveCenter(newCurrentLocation: Position) {
        if (currentPosition.isSomewhereElse(newCurrentLocation)) {
            currentPositionLiveData.postValue(newCurrentLocation)
            centerLiveData.postValue(Center(newCurrentLocation, Center.Source.MODEL))
        }
    }

    /*
        Called when the map was scrolled (scroll or fling)
        - map: OnScroll
        - update center live data
        - fragment observes live data
        --> to update the subtitle and display the coordinates of the new map center
        Mind:
        a) the map center must not be changed, or finging would not work properly
        b) location tracking must be disabled when the map is touched
     */
    internal fun onScrollSetNewCenter(point: IGeoPoint) {
        if (center.isSomewhereElse(point)) {
            val position = Position(point.latitude, point.longitude, center.altitude)
            val center = Center(position, Center.Source.SCROLLING)
            centerLiveData.postValue(center)
        }
    }

    /*
        Called in reaction on a button click (set home, set current location) or when tracking the current geo location
        - update center live data
        - fragment observes live data
        --> to update the subtitle and display the coordinates of the new map center
        --> to change the map center
     */
    internal fun setNewCenter(newCenter: Position) {
        if (center.isSomewhereElse(newCenter)) {
            val center = Center(newCenter, Center.Source.MODEL)
            centerLiveData.postValue(center)
        }
    }

    internal fun setCurrentLocation(newCurrentLocation: Position) {
        if (currentPosition.isSomewhereElse(newCurrentLocation)) {
            currentPositionLiveData.postValue(newCurrentLocation)
        }
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
            currentPositionLiveData.value = currentPosition
            centerLiveData.value = Center(Position(latitude, longitude, altitude), source = Center.Source.MODEL)
            zoomLiveData.value = zoomLevel
        }
    }

    internal fun save(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
        currentPosition.also {
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
        currentPositionLiveData.postValue(currentPosition)
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
