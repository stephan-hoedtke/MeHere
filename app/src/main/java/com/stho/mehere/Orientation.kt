package com.stho.nyota.sky.utilities

import com.stho.mehere.Degree
import kotlin.math.IEEErem


/**
 * Orientation: angles in degree (azimuth, pitch, roll)
 *
 * Project the positive y-axis [from the bottom edge to the top edge of the phone] to the sphere
 *      - azimuth: the angle to the geographic north at the horizon plane
 *      - pitch: the angle downwards when the top edge is tilted down
 *      - roll: the angle downwards when the left edge is tilted down
 *
 * Project the negative z-axis [how your eyes look into the phone] to the spheres
 *      - center azimuth: the angle to the geographic north at the horizon plane
 *      - center altitude: the angle upwards or downwards from the horizon
 *
 * Pitch (around X axis):
 *   When the device is placed face up on a table, the pitch value is 0.
 *   When the positive Z axis begins to tilt towards the positive Y axis, the pitch angle becomes positive.
 *   (This is when the top edge of the device is moving downwards)
 *   The value of Pitch ranges from -180 degrees to 180 degrees.
 *
 * Roll (around Y axis):
 *   When the device is placed face up on a table, the roll value is 0.
 *   When the positive X axis begins to tilt towards the positive Z axis, the roll angle becomes positive.
 *   (This is when the left edge of the device is moving downwards)
 *   The value of Roll ranges from -90 degrees to 90 degrees.
 *
 * Azimuth (around Z axis):
 *   The following table shows the value of Azimuth when the positive Y axis of the device is aligned to the magnetic north, south, east, and west
 *      North -> 0
 *      East -> 90
 *      South -> 180
 *      West -> 270
 */
data class Orientation(val azimuth: Double, val pitch: Double, val roll: Double, val centerAzimuth: Double, val centerAltitude: Double) {

    /**
     * Altitude (top edge of the device pointing upwards) is the opposite of pitch (top edge of the device pointing downwards)
     */
    val altitude: Double by lazy { -pitch }

    fun getRotationToTargetAt(targetAzimuth: Double): Float =
        Degree.difference(x = targetAzimuth, y = azimuth).toFloat()

    fun getRotationToNorth(): Float =
        Degree.normalizeTo180(-azimuth).toFloat()

    fun normalize() =
        if (roll < -90 || roll > 90) {
            Orientation(
                azimuth = azimuth + 180,
                pitch = 180 - pitch,
                roll = roll - 180,
                centerAzimuth = centerAzimuth,
                centerAltitude = centerAltitude
            )
        } else {
            this
        }

    companion object {

        val default: Orientation =
            Orientation(
                azimuth = 0.0,
                pitch = 0.0,
                roll = 0.0,
                centerAzimuth = 0.0,
                centerAltitude = -90.0
            )
    }
}
