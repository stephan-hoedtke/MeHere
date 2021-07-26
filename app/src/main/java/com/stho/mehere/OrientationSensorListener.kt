package com.stho.mehere

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Display
import android.view.Surface
import com.stho.nyota.sky.utilities.*
import android.view.WindowManager as WindowManager1



class OrientationSensorListener(private val context: Context, private val filter: IOrientationFilter) : SensorEventListener {

    interface IOrientationFilter {
        fun onOrientationAnglesChanged(orientation: Quaternion)
        val currentOrientation: Orientation
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager: WindowManager1 = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager1
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val gyroscopeReading = FloatArray(3)
    private var display: Display? = null
    private val timer: Timer = Timer()
    private var hasMagnetometer: Boolean = false
    private var hasAccelerationMagnetometer: Boolean = false
    private var hasGyro: Boolean = false
    private var accelerationMagnetometerOrientation: Quaternion = Quaternion.default
    private var estimate: Quaternion = Quaternion.default

    internal fun onResume() {
        // TODO: for API30 you shall user: context.display.rotation
        display = windowManager.defaultDisplay
        initializeRotationVectorSensor()
    }

    internal fun onPause() {
        display = null
        removeSensorListeners()
    }

    /**
     * The rotation vector sensor is fusing gyroscope, accelerometer and magnetometer. No further sensor fusion required.
     */
    private fun initializeRotationVectorSensor() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME)

        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME)

        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun removeSensorListeners() =
        sensorManager.unregisterListener(this)

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // We don't care
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                updateOrientationAnglesFromAcceleration()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometer = true
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyroscopeReading, 0, gyroscopeReading.size)
                updateOrientationAnglesFromGyroscope()
            }
        }
    }

    private fun updateOrientationAnglesFromAcceleration() {
        // If magnetometer is not initialized yet, don't continue...
        if (!hasMagnetometer)
            return

        val rotationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            // TODO: get quaternion from readings directly
            val adjustedRotationMatrix = RotationMatrix.fromFloatArray(getAdjustedRotationMatrix(rotationMatrix))
            accelerationMagnetometerOrientation = Quaternion.fromRotationMatrix(adjustedRotationMatrix)
            hasAccelerationMagnetometer = true
        }
    }

    private fun updateOrientationAnglesFromGyroscope() {
        // If acceleration is not initialized yet, don't continue...
        if (!hasAccelerationMagnetometer)
            return

        // If gyro is not initialized yet, do it now...
        if (!hasGyro) {
            estimate = Quaternion.default * accelerationMagnetometerOrientation
            hasGyro = true
        }

        val dt = timer.getNextTime()
        if (dt > 0) {
            filterUpdate(dt)
            filter.onOrientationAnglesChanged(estimate)
        }
    }

    private fun filterUpdate(dt: Double) {

        val omega = Vector.fromFloatArray(gyroscopeReading)

        // Get updated Gyro delta rotation from gyroscope readings
        val deltaRotation = Rotation.getRotationFromGyro(omega, dt)

        // update the gyro orientation
        val gyroOrientation = estimate * deltaRotation

        // fuse sensors
        estimate = Quaternion.interpolate(gyroOrientation, accelerationMagnetometerOrientation, DEFAULT_FILTER_COEFFICIENT)
    }

    /**
     * See the following training materials from google.
     * https://codelabs.developers.google.com/codelabs/advanced-android-training-sensor-orientation/index.html?index=..%2F..advanced-android-training#0
     */
    private fun getAdjustedRotationMatrix(rotationMatrix: FloatArray): FloatArray {
        val adjustedRotationMatrix = FloatArray(9)
        when (display?.rotation) {
            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, adjustedRotationMatrix)
                return adjustedRotationMatrix
            }
            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, adjustedRotationMatrix)
                return adjustedRotationMatrix
            }
            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, adjustedRotationMatrix)
                return adjustedRotationMatrix
            }
            else -> {
                return rotationMatrix
            }
        }
    }

    companion object {
        private const val DEFAULT_FILTER_COEFFICIENT: Double = 0.001
    }
}