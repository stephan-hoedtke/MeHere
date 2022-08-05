package com.stho.mehere

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.preference.PreferenceManager
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import java.util.function.BiPredicate

class Repository {

    internal class Center(val location: Location, val source: Source) {

        enum class Source {
            SCROLLING,
            MODEL,
        }

        internal fun isSomewhereElse(point: IGeoPoint): Boolean =
            location.isSomewhereElse(point)

        internal fun toGeoPoint(): GeoPoint =
            location.toGeoPoint()
    }

    private val isDirtyLiveData = MutableLiveData<Boolean>().apply { value = false }
    private val currentLocationLiveData = MutableLiveData<Location>().apply { value = defaultLocationBerlinBuch  }
    private val centerLiveData = MutableLiveData<Center>().apply { value = defaultCenterBerlinBuch }
    private val zoomLiveData = MutableLiveData<Double>().apply { value = defaultZoom }
    private val pointsLiveData = MutableLiveData<MutableMap<String, PointOfInterest>>()

    internal val currentLocationLD: LiveData<Location>
        get() = currentLocationLiveData

    internal val currentLocation: Location
        get() = currentLocationLiveData.value ?: defaultLocationBerlinBuch

    internal val centerLD: LiveData<Center>
        get() = centerLiveData

    internal val center: Location
        get() = centerLiveData.value?.location ?: defaultLocationBerlinBuch

    internal fun setCurrentLocationMoveCenter(newCurrentLocation: Location) {
        if (currentLocation.isSomewhereElse(newCurrentLocation)) {
            currentLocationLiveData.postValue(newCurrentLocation)
            centerLiveData.postValue(Center(newCurrentLocation, Center.Source.MODEL))
        }
    }

    internal val isDirtyLD: LiveData<Boolean>
        get() = isDirtyLiveData

    internal var isDirty: Boolean
        get() = isDirtyLiveData.value ?: false
        set(value) {
            if (isDirtyLiveData.value != value) {
                isDirtyLiveData.postValue(value)
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
            val location = Location(point.latitude, point.longitude, center.altitude)
            val center = Center(location, Center.Source.SCROLLING)
            centerLiveData.postValue(center)
            isDirty = true
        }
    }

    /*
        Called in reaction on a button click (set home, set current location) or when tracking the current geo location
        - update center live data
        - fragment observes live data
        --> to update the subtitle and display the coordinates of the new map center
        --> to change the map center
     */
    internal fun setNewCenter(newCenter: Location) {
        if (center.isSomewhereElse(newCenter)) {
            val center = Center(newCenter, Center.Source.MODEL)
            centerLiveData.postValue(center)
            isDirty = true
        }
    }

    internal fun setCurrentLocation(newCurrentLocation: Location) {
        if (currentLocation.isSomewhereElse(newCurrentLocation)) {
            currentLocationLiveData.postValue(newCurrentLocation)
            isDirty = true
        }
    }

    internal val zoomLD: LiveData<Double>
        get() = zoomLiveData

    internal var zoom: Double
        get() = zoomLiveData.value ?: defaultZoom
        set(value) {
            if (zoomLiveData.value != value) {
                zoomLiveData.postValue(value)
                isDirty = true
            }
        }

    internal val pointsLD: LiveData<Collection<PointOfInterest>>
        get() = Transformations.map(pointsLiveData) { map -> map.values }

    internal val points: MutableMap<String, PointOfInterest>
        get() = pointsLiveData.value ?: hashMapOf();

    internal fun create(point: PointOfInterest) {
        points[point.id] = point
        pointsLiveData.postValue(points);
        isDirty = true
    }

    internal fun update(id: String, point: PointOfInterest) {
        points.replace(id, point)
        pointsLiveData.postValue(points);
        isDirty = true
    }

    internal fun delete(id: String) {
        points.remove(id)
        pointsLiveData.postValue(points)
        isDirty = true
    }

    private fun load(context: Context) {
        Storage(context).also { storage ->
            val center = storage.readCenter(currentLocation)
            val points = storage.readPoints().map { it.id to it }.toMap().toMutableMap()
            zoomLiveData.value = storage.readZoom(defaultZoom)
            pointsLiveData.value = points
            currentLocationLiveData.value = currentLocation
            centerLiveData.value = Center(center, source = Center.Source.MODEL)
            isDirty = false
        }
    }

    internal fun save(context: Context) {
        Storage(context).also { storage ->
            storage.writeCenter(currentLocation)
            storage.writeZoom(zoom)
            storage.writePoints(points.values.toList())
            isDirty = false
        }
    }

    internal fun touch() {
        currentLocationLiveData.postValue(currentLocation)
    }

    companion object {

        internal const val defaultLatitude = 52.64511
        internal const val defaultLongitude = 13.49181
        internal const val defaultAltitude = 90.0
        internal const val defaultZoom = 13.0

        internal val defaultLocationBerlinBuch: Location by lazy {
            Location(defaultLatitude, defaultLongitude, defaultAltitude)
        }

        internal val defaultCenterBerlinBuch: Center by lazy {
            Center(defaultLocationBerlinBuch, Center.Source.MODEL)
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
