package com.stho.nyota.sky.utilities

import com.stho.mehere.Degree
import com.stho.mehere.RotationMatrix
import kotlin.math.*

object Rotation {

    /**
     * Returns the orientation (azimuth, pitch, roll, center azimuth, center altitude) of the device
     *      for a rotation from sensor frame into earth frame
     *
     *      (C) The center "pointer" vector is defined by
     *              C = M * (0, 0, -1)
     *
     *          --> C = (-m13, -m23, -m33)
     *
     *              center azimuth  = atan2(-m13, -m23)
     *              center azimuth = asin(-m33) // opposite of pitch
     */
    internal fun getOrientationFor(r: RotationMatrix): Orientation =
        if (isGimbalLockForSinus(r.m32)) {
            if (r.m32 < 0) { // pitch 90°
                val roll = Degree.arcTan2(r.m21, r.m23)
                Orientation(
                    azimuth = 0.0,
                    pitch = 90.0,
                    roll = roll,
                    centerAzimuth = 180 - roll,
                    centerAltitude = 0.0,
                )
            } else { // pitch -90°
                val roll = Degree.arcTan2(-r.m21, -r.m23)
                Orientation(
                    azimuth = 0.0,
                    pitch = -90.0,
                    roll = roll,
                    centerAzimuth = roll,
                    centerAltitude = 0.0,
                )
            }
        } else {
            if (isGimbalLockForCenter(r.m13, r.m23)) { // pitch 0°
                val azimuth = Degree.arcTan2(r.m12, r.m22)
                val roll = Degree.arcTan2(r.m31, r.m33)
                Orientation(
                    azimuth = azimuth,
                    pitch = Degree.arcSin(-r.m32),
                    roll = roll,
                    centerAzimuth = azimuth,
                    centerAltitude = roll - 90,
                )
            }
            else {
                Orientation(
                    azimuth = Degree.arcTan2(r.m12, r.m22),
                    pitch = Degree.arcSin(-r.m32),
                    roll = Degree.arcTan2(r.m31, r.m33),
                    centerAzimuth = Degree.arcTan2(-r.m13, -r.m23),
                    centerAltitude = Degree.arcSin(-r.m33),
                )
            }
        }

    /**
     * Returns the rotation matrix as integration of angle velocity from gyroscope of a time period
     *
     * @param omega angle velocity around x, y, z, in radians/second
     * @param dt time period in seconds
     */
    internal fun getRotationFromGyro(omega: Vector, dt: Double): Quaternion {
        // Calculate the angular speed of the sample
        val omegaMagnitude: Double = omega.length

        // Normalize the rotation vector if it's big enough to get the axis
        // (that is, EPSILON should represent your maximum allowable margin of error)
        val w = if (omegaMagnitude > OMEGA_THRESHOLD)
            omega.div(omegaMagnitude)
        else
            omega

        // Quaternion integration:
        // ds/dt = omega x s
        // with s = q # s0 # q* follows
        //      dq/dt = 0.5 * omega # q
        //      q(t) = exp(0.5 * omega * (t - t0)) # q0
        //      q(t) = cos(|v|) + v / |v| * sin(|v|) # q0 with v = 0.5 * omega * (t - t0)
        //      this is equivalent to a rotation by theta around the rotation vector omega/|omega| with theta = |omega| * (t - t0)
        val theta: Double = omegaMagnitude * dt
        return Quaternion.forRotation(w, theta)
    }


    /**
     * Returns if sin(x) is about +/- 1.0
     */
    private fun isGimbalLockForSinus(sinX: Double): Boolean =
        sinX < GIMBAL_LOCK_SINUS_MINIMUM || sinX > GIMBAL_LOCK_SINUS_MAXIMUM

    /**
     * Returns if x^2 +y^2 is too small to calculate the atan2
     */
    private fun isGimbalLockForCenter(sinX: Double, cosX: Double): Boolean =
        abs(sinX) < GIMBAL_LOCK_SINUS_TOLERANCE && abs(cosX) < GIMBAL_LOCK_SINUS_TOLERANCE

    /**
     * When the pitch is about 90° (Gimbal lock) the rounding errors of x, y, z produce unstable azimuth and roll
     *      pitch = +/- 90°
     *      --> z = +/- 1.0
     *          x = +/- 0.0
     *          y = +/- 0.0
     *      --> atan2(...,...) can be anything.
     *
     * Tolerance estimation:
     *      x,y < 0.001 --> z > sqrt(1 - x * x - y * y) = sqrt(0.999998) = 0.999999 --> 89.92°
     *          pitch = +/- (90° +/- 0.08°) or
     *          pitch = +/- (PI/2 +/- 0.001414) or
     *          sin(x) = +/- (1.0 +/- 0.000001)
     *
     */
    private const val GIMBAL_LOCK_SINUS_TOLERANCE: Double = 0.000001
    private const val GIMBAL_LOCK_SINUS_MINIMUM: Double = -0.999999
    private const val GIMBAL_LOCK_SINUS_MAXIMUM: Double = 0.999999
    private const val OMEGA_THRESHOLD: Double = 0.0000001
}

