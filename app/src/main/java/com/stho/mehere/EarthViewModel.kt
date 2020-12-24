package com.stho.mehere

import android.app.Application
import android.location.Location
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.osmdroid.api.IGeoPoint


class EarthViewModel(application: Application, private val repository: Repository, private val settings: Settings) : AndroidViewModel(application) {

    private var isTrackingEnabled = true
    private val acceleration: Acceleration = Acceleration()
    private val lowPassFilter: LowPassFilter = LowPassFilter()
    private val northPointerLiveData = MutableLiveData<Double>()
    private val homeLiveData = MutableLiveData<Position>()
    private val networkStatusLiveData = MutableLiveData<NetworkStatus>()
    private val alphaLiveData = MutableLiveData<Float>()

    init {
        networkStatusLiveData.value = NetworkStatus.OFFLINE
        northPointerLiveData.value = 30.0
        homeLiveData.value = settings.home
        alphaLiveData.value = settings.alpha
    }

    internal val locationLD: LiveData<Repository.MyPosition>
        get() = repository.locationLD

    internal val currentLocationLD: LiveData<Position>
        get() = Transformations.map(repository.locationLD) { location -> location.currentLocation }

    internal val centerLD: LiveData<Position>
        get() = Transformations.map(repository.locationLD) { location -> location.center }

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

    internal val alphaLD: LiveData<Float>
        get() = alphaLiveData

    internal val networkStatusLD: LiveData<NetworkStatus>
        get() = networkStatusLiveData

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

    internal var alpha: Float
        get() = alphaLiveData.value ?: 0.5f
        set(value) {
            alphaLiveData.postValue(value)
            settings.alpha = value
        }

    internal fun enableTrackingDelayed() {
        if (!isTrackingEnabled) {
            Handler().postDelayed({
                isTrackingEnabled = true
                repository.touch()
            }, 1000)
        }
    }

    internal fun disableTracking() {
        if (isTrackingEnabled) {
            isTrackingEnabled = false
            repository.touch()
        }
    }

    internal fun update(orientationAngles: FloatArray) {
        val gravity: Vector = lowPassFilter.setAcceleration(orientationAngles)
        updateAcceleration(-Degree.fromRadian(gravity.x))
    }

    private fun updateAcceleration(newAngle: Double) {
        acceleration.rotateTo(newAngle)
    }

    internal fun updateCurrentLocation(location: Location) {
        updateCurrentLocation(Position(location))
    }

    private fun updateCurrentLocation(newCurrentLocation: Position) {
        if (repository.currentLocation.isSomewhereElse(newCurrentLocation)) {
            if (useTracking) {
                repository.setCurrentLocationMoveCenter(newCurrentLocation)
            } else {
                repository.setCurrentLocationKeepCenter(newCurrentLocation)
            }
        }
    }

    internal fun updateCenter(point: IGeoPoint) {
        repository.setCenterKeepCurrentLocation(
            Position(
                point.latitude,
                point.longitude,
                center.altitude
            )
        )
    }

    internal fun updateCenterAndDisableTracking(newCenter: Position) {
        disableTracking()
        repository.setCenterKeepCurrentLocation(newCenter)
    }

    internal fun updateCenterToCurrentLocationAndEnableTracking() {
        repository.setCurrentLocationMoveCenter(currentLocation)
        enableTrackingDelayed()
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
        repository.setCurrentLocationMoveCenter(Repository.defaultLocationBerlinBuch)
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

    internal fun setNetworkStatus(status: NetworkStatus) {
        networkStatusLiveData.postValue(status)
    }

    companion object {
        internal const val maxZoom: Double = 21.0
        internal const val minZoom: Double = 2.0
    }
}