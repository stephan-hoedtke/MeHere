package com.stho.mehere

import android.app.Application
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlin.math.PI

class EarthViewModel(application: Application) : AndroidViewModel(application) {

    private val acceleration: Acceleration = Acceleration()
    private val lowPassFilter: LowPassFilter = LowPassFilter()

    private val locationLiveData = MutableLiveData<Location>()
    private val northPointerLiveData = MutableLiveData<Double>()
    private val zoomLevelLiveData = MutableLiveData<Double>()

    internal val maxZoomLevel: Double = 19.0
    internal val minZoomLevel: Double = 2.0

    internal val locationLD: LiveData<Location>
        get() = locationLiveData

    internal val northPointerLD: LiveData<Double>
        get() = northPointerLiveData

    internal val zoomLevelLD: LiveData<Double>
        get() = zoomLevelLiveData

    internal val canZoomInLD: LiveData<Boolean>
        get() = Transformations.map(zoomLevelLiveData) { zoomLevel -> zoomLevel < maxZoomLevel }

    internal val canZoomOutLD: LiveData<Boolean>
        get() = Transformations.map(zoomLevelLiveData) { zoomLevel -> zoomLevel > minZoomLevel }

    init {
        locationLiveData.value = defaultLocationBerlinBuch
        northPointerLiveData.value = 30.0
        zoomLevelLiveData.value = defaultZoomLevel
    }

    internal fun update(orientationAngles: FloatArray) {
        val gravity: Vector = lowPassFilter.setAcceleration(orientationAngles)
        updateAcceleration(-180 * gravity.x / PI)
    }

    private fun updateAcceleration(newAngle: Double) {
        val angle = rotateTo(acceleration.position, newAngle)
        acceleration.update(angle)
    }

    internal fun update(location: Location) {
        locationLiveData.postValue(location)
    }

    internal fun zoomIn() {
        val newZoomLevel = (zoomLevelLiveData.value ?: defaultZoomLevel) + 1.0
        if (newZoomLevel <= maxZoomLevel) {
            zoomLevelLiveData.value = newZoomLevel
        }
    }

    internal fun zoomOut() {
        val newZoomLevel = (zoomLevelLiveData.value ?: defaultZoomLevel) - 1.0
        if (newZoomLevel >= minZoomLevel) {
            zoomLevelLiveData.value = newZoomLevel
        }
    }

    fun updateNorthPointer() {
        northPointerLiveData.postValue(acceleration.position)
    }

    companion object {
        internal val defaultLocationBerlinBuch: Location
            get() = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = 52.633497466
                longitude = 13.492831362
                altitude = 90.0 // in m above see  level
            }

        private val defaultZoomLevel: Double = 5.0

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