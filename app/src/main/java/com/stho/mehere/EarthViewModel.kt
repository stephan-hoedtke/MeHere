package com.stho.mehere

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import kotlin.math.PI

class EarthViewModel(application: Application, private val repository: Repository, private val settings: Settings) : AndroidViewModel(application) {

    private var isTrackingEnabled = false
    private val acceleration: Acceleration = Acceleration()
    private val lowPassFilter: LowPassFilter = LowPassFilter()
    private val northPointerLiveData = MutableLiveData<Double>()
    private val homeLiveData = MutableLiveData<Location>()

    internal val locationLD: LiveData<Location>
        get() = repository.currentLocationLD

    internal val centerLD: LiveData<GeoPoint>
        get() = repository.centerLD

    internal val zoomLevelLD: LiveData<Double>
        get() = repository.zoomLevelLD

    internal val northPointerLD: LiveData<Double>
        get() = northPointerLiveData

    internal val canZoomInLD: LiveData<Boolean>
        get() = Transformations.map(zoomLevelLD) { zoomLevel -> zoomLevel < maxZoomLevel }

    internal val canZoomOutLD: LiveData<Boolean>
        get() = Transformations.map(zoomLevelLD) { zoomLevel -> zoomLevel > minZoomLevel }

    internal val homeLD: LiveData<Location>
        get() = homeLiveData

    internal val location: Location
        get() = repository.currentLocation

    internal val home: Location
        get() = settings.home

    internal val center: GeoPoint
        get() = repository.center

    internal val useLocation: Boolean
        get() = settings.useLocation

    internal val useOrientation: Boolean
        get() = settings.useOrientation

    internal val useTracking: Boolean
        get() = settings.useTracking && isTrackingEnabled

    internal fun disableTracking() {
        isTrackingEnabled = false
    }

    internal fun enableTracking() {
        isTrackingEnabled = true
    }

    init {
        northPointerLiveData.value = 30.0
        homeLiveData.value = settings.home
        isTrackingEnabled = true
    }

    internal fun update(orientationAngles: FloatArray) {
        val gravity: Vector = lowPassFilter.setAcceleration(orientationAngles)
        updateAcceleration(-180 * gravity.x / PI)
    }

    private fun updateAcceleration(newAngle: Double) {
        val angle = rotateTo(acceleration.position, newAngle)
        acceleration.update(angle)
    }

    internal fun updateCurrentLocation(location: Location) {
        repository.currentLocation = location
    }

    internal fun updateCenter(center: IGeoPoint) {
        repository.center.also {
            if (it.latitude != center.latitude || it.longitude != center.longitude) {
                it.latitude = center.latitude
                it.longitude = center.longitude
                repository.center = it
            }
        }
    }

    internal fun zoomIn() {
        val newZoomLevel = repository.zoomLevel + 1.0
        if (newZoomLevel <= maxZoomLevel) {
            repository.zoomLevel = newZoomLevel
        }
    }

    internal fun zoomOut() {
        val newZoomLevel = repository.zoomLevel - 1.0
        if (newZoomLevel >= minZoomLevel) {
            repository.zoomLevel = newZoomLevel
        }
    }

    internal fun setZoomLevel(zoomLevel: Double) {
        repository.zoomLevel = zoomLevel
    }

    internal fun updateNorthPointer() {
        acceleration.position.also {
            if (northPointerLiveData.value != it)
                northPointerLiveData.postValue(it)
        }
    }

    internal fun reset() {
        repository.zoomLevel = Repository.defaultZoomLevel
        repository.currentLocation = Repository.defaultLocationBerlinBuch
        northPointerLiveData.postValue(0.0)
    }

    internal fun setHome(point: IGeoPoint) {
        settings.home.latitude = point.latitude
        settings.home.longitude = point.longitude
        settings.save(getApplication())
        homeLiveData.postValue(settings.home)
    }

    internal fun loadSettings() {
        settings.load(getApplication())
    }

    internal fun save() {
        repository.save(getApplication())
        settings.save(getApplication())
    }

    companion object {
        internal const val maxZoomLevel: Double = 21.0
        internal const val minZoomLevel: Double = 2.0

        private fun rotateTo(from: Double, to: Double): Double {
            val difference: Double = getAngleDifference(from, to)
            return from + difference
        }

        private fun getAngleDifference(from: Double, to: Double): Double {
            var alpha = to - from
            while (alpha > +180) alpha -= 360f
            while (alpha < -180) alpha += 360f
            return alpha
        }
    }
}