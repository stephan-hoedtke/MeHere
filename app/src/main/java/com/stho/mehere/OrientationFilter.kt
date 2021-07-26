package com.stho.nyota

import com.stho.mehere.OrientationSensorListener
import com.stho.nyota.sky.utilities.Orientation
import com.stho.nyota.sky.utilities.Quaternion
import com.stho.nyota.sky.utilities.QuaternionAcceleration


/*
    The class takes updates of the orientation vector by listening to onOrientationChanged(rotationMatrix).
    The values will be stored and smoothed with acceleration.
    A handler will regularly read the updated smoothed orientation
 */
class OrientationAccelerationFilter: OrientationSensorListener.IOrientationFilter {

    private val acceleration: QuaternionAcceleration = QuaternionAcceleration(DEFAULT_ACCELERATION_FACTOR)

    override val currentOrientation: Orientation
        get() = acceleration.position.toOrientation().normalize()

    override fun onOrientationAnglesChanged(orientation: Quaternion) {
        acceleration.rotateTo(orientation)
    }

    companion object {
        private const val DEFAULT_ACCELERATION_FACTOR: Double = 0.25
    }
}


