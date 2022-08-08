package com.stho.mehere

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.*
import com.stho.nyota.sky.utilities.Orientation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.api.IGeoPoint
import kotlin.math.abs


class EarthViewModel(application: Application, private val repository: Repository, private val settings: Settings) : AndroidViewModel(application) {

    private var isTrackingEnabled: Boolean = false
    private val northPointerLiveData = MutableLiveData<Double>().apply { value = 30.0 }
    private val homeLiveData = MutableLiveData<Location>().apply { value = settings.home }
    private val networkStatusLiveData = MutableLiveData<NetworkStatusInfo>().apply { value = NetworkStatusInfo() }
    private val alphaLiveData = MutableLiveData<Float>().apply { value = settings.alpha }
    private var showCompassLiveData = MutableLiveData<Boolean>().apply { value = true }
    private var showCurrentLocationLiveData = MutableLiveData<Boolean>().apply { value = true }
    private val captureModeLiveData = MutableLiveData<CaptureMode>().apply { value = CaptureMode.NORMAL }

    internal val currentLocationLD: LiveData<Location>
        get() = repository.currentLocationLD

    internal val centerLD: LiveData<Repository.Center>
        get() = repository.centerLD

    internal val zoomLD: LiveData<Double>
        get() = repository.zoomLD

    internal val northPointerLD: LiveData<Double>
        get() = northPointerLiveData

    internal val canZoomInLD: LiveData<Boolean>
        get() = Transformations.map(zoomLD) { zoomLevel -> zoomLevel < maxZoom }

    internal val canZoomOutLD: LiveData<Boolean>
        get() = Transformations.map(zoomLD) { zoomLevel -> zoomLevel > minZoom }

    internal val homeLD: LiveData<Location>
        get() = homeLiveData

    internal val alphaLD: LiveData<Float>
        get() = alphaLiveData

    internal val networkStatusLD: LiveData<NetworkStatusInfo>
        get() = networkStatusLiveData

    internal val pointsLD: LiveData<Collection<PointOfInterest>>
        get() = repository.pointsLD

    internal val currentLocation: Location
        get() = repository.currentLocation

    internal val home: Location
        get() = settings.home

    internal val useMapsForge: Boolean
        get() = settings.useMapsForge

    internal val scaleFonts: Boolean
        get() = settings.scaleFonts

    internal val center: Location
        get() = repository.center

    internal val zoom: Double
        get() = repository.zoom

    internal val useLocation: Boolean
        get() = settings.useLocation

    internal val useOrientation: Boolean
        get() = settings.useOrientation

    internal val useTracking: Boolean
        get() = settings.useTracking && isTrackingEnabled

    internal val rotateMapView: Boolean
        get() = settings.rotateMapView

    internal var alpha: Float
        get() = alphaLiveData.value ?: 0.5f
        set(value) {
            alphaLiveData.postValue(value)
            settings.alpha = value
        }

    internal val captureModeLD: LiveData<CaptureMode>
        get() = captureModeLiveData

    internal var captureMode: CaptureMode
        get() = captureModeLiveData.value ?: CaptureMode.NORMAL
        set(value) {
            if (captureModeLiveData.value != value) {
                captureModeLiveData.postValue(value)
            }
        }

    internal fun toggleMode() {
        captureMode = when(captureMode) {
            CaptureMode.NORMAL -> CaptureMode.CAPTURE
            CaptureMode.CAPTURE -> CaptureMode.NORMAL
        }
    }

    private fun stopCapture() {
        captureMode = CaptureMode.NORMAL
    }

    internal fun getModeTitle(captureMode: CaptureMode): Int =
        when(captureMode) {
            CaptureMode.NORMAL -> R.string.action_start_capture
            CaptureMode.CAPTURE -> R.string.action_stop_capture
        }

    private fun enableTrackingDelayed() {
        if (!isTrackingEnabled) {
            viewModelScope.launch {
                delay(1000)
                isTrackingEnabled = true
                repository.touch()
            }
        }
    }

    internal fun disableTracking() {
        isTrackingEnabled = false
        repository.touch()
    }

    internal fun updateLocation(location: Location) =
        onLocationChangedSetCurrentLocation(newCurrentLocation = location)

    private fun onLocationChangedSetCurrentLocation(newCurrentLocation: Location) {
        if (repository.currentLocation.isSomewhereElse(newCurrentLocation)) {
            if (isTrackingEnabled) {
                repository.setCurrentLocationMoveCenter(newCurrentLocation)
            } else {
                repository.setCurrentLocation(newCurrentLocation)
            }
        }
    }

    internal fun onScrollSetNewCenter(point: IGeoPoint) =
        repository.onScrollSetNewCenter(point)

    internal fun updateCenterAndDisableTracking(newCenter: Location) {
        disableTracking()
        repository.setNewCenter(newCenter)
    }

    internal fun updateCenterToCurrentLocationAndEnableTracking() {
        enableTrackingDelayed()
        repository.setNewCenter(currentLocation)
    }

    internal fun zoomIn() =
        updateZoom(repository.zoom + 1.0)

    internal fun zoomOut() =
        updateZoom(repository.zoom - 1.0)

    internal fun updateZoom(zoom: Double) {
        repository.zoom = zoom.coerceIn(minZoom, maxZoom)
    }

    internal fun updateOrientation(orientation: Orientation) =
        setNorthPointer(-orientation.azimuth)

    private fun setNorthPointer(newValue: Double) {
        val oldValue = northPointerLiveData.value ?: 0.0
        if (abs(newValue - oldValue) > 0.00000001) {
            northPointerLiveData.postValue(newValue)
        }
    }

    internal fun reset() {
        repository.zoom = Repository.defaultZoom
        repository.setCurrentLocationMoveCenter(Repository.defaultLocationBerlinBuch)
        northPointerLiveData.postValue(0.0)
    }

    internal fun setHome(home: Location) {
        settings.home = home
        settings.save(getApplication())
        homeLiveData.postValue(home)
    }

    internal fun loadSettings() =
        settings.load(getApplication())

    internal fun save() {
        repository.save(getApplication())
        settings.save(getApplication())
    }

    internal val networkStatus: NetworkStatusInfo
        get() = networkStatusLiveData.value ?: NetworkStatusInfo()

    internal fun setNetworkAvailable(connectivityManager: ConnectivityManager, network: Network) {
        networkStatus.also {
            it.setAvailable(connectivityManager, network)
            networkStatusLiveData.postValue(it)
        }
    }

    internal fun setNetworkLost(network: Network) {
        networkStatus.also {
            it.setLost(network)
            networkStatusLiveData.postValue(it)
        }
    }

    internal fun setNetworkUnavailable() {
        networkStatus.also {
            it.setUnavailable()
            networkStatusLiveData.postValue(it)
        }
    }

    companion object {
        internal const val maxZoom: Double = 21.0
        internal const val minZoom: Double = 2.0
    }
}
