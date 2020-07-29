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
    private val homeLiveData = MutableLiveData<Position>()

    internal val locationLD: LiveData<Position>
        get() = repository.currentLocationLD

    internal val centerLD: LiveData<Position>
        get() = repository.centerLD

    internal val zoomLD: LiveData<Double>
        get() = repository.zoomLD

    internal val northPointerLD: LiveData<Double>
        get() = northPointerLiveData

    internal val canZoomInLD: LiveData<Boolean>
        get() = Transformations.map(zoomLD) { zoomLevel -> zoomLevel < maxZoom }

    internal val canZoomOutLD: LiveData<Boolean>
        get() = Transformations.map(zoomLD) { zoomLevel -> zoomLevel > minZoom }

    internal val homeLD: LiveData<Position>
        get() = homeLiveData

    internal val currentLocation: Position
        get() = repository.currentLocation

    internal val home: Position
        get() = settings.home

    internal val center: Position
        get() = repository.center

    internal val zoom: Double
        get() = repository.zoom

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
        if (repository.currentLocation.isSomewhereElse(location)) {
            repository.currentLocation = Position(location)
        }
    }

    internal fun updateCenter(point: IGeoPoint) {
        if (repository.center.isSomewhereElse(point)) {
            repository.center = Position(point.latitude, point.longitude, repository.center.altitude)
        }
    }

    internal fun updateCenter(position: Position) {
        if (repository.center.isSomewhereElse(position)) {
            repository.center = position
        }
    }

    internal fun zoomIn() {
        val newZoom = repository.zoom + 1.0
        if (newZoom <= maxZoom) {
            repository.zoom = newZoom
        }
    }

    internal fun zoomOut() {
        val newZoom = repository.zoom - 1.0
        if (newZoom >= minZoom) {
            repository.zoom = newZoom
        }
    }

    internal fun updateZoom(zoom: Double) {
        repository.zoom = zoom
    }

    internal fun updateNorthPointer() {
        acceleration.position.also {
            if (northPointerLiveData.value != it)
                northPointerLiveData.postValue(it)
        }
    }

    internal fun reset() {
        repository.zoom = Repository.defaultZoom
        repository.currentLocation = Repository.defaultLocationBerlinBuch
        northPointerLiveData.postValue(0.0)
    }

    internal fun setHome(home: Position) {
        settings.home = home
        settings.save(getApplication())
        homeLiveData.postValue(home)
    }

    internal fun loadSettings() {
        settings.load(getApplication())
    }

    internal fun save() {
        repository.save(getApplication())
        settings.save(getApplication())
    }

    companion object {
        internal const val maxZoom: Double = 21.0
        internal const val minZoom: Double = 2.0

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