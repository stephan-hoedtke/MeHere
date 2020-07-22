package com.stho.mehere

import android.os.SystemClock

internal class LowPassFilter {
    private val gravity: Vector = Vector(0.0, 0.0, 9.78)
    private var startTimeNanos: Long = 0
    private var count: Long = 0

    fun setAcceleration(acceleration: FloatArray): Vector {
        val dt = averageTimeDifferenceInSeconds
        lowPassFilter(acceleration, dt)
        return gravity
    }

    fun reset() {
        gravity.x = 0.0
        gravity.y = 0.0
        gravity.z = 9.78
        count = 0
    }

    private fun lowPassFilter(acceleration: FloatArray, dt: Float) {
        if (dt > 0) {
            val alpha = dt / (TIME_CONSTANT + dt)
            gravity.x += alpha * (acceleration[0] - gravity.x)
            gravity.y += alpha * (acceleration[1] - gravity.y)
            gravity.z += alpha * (acceleration[2] - gravity.z)
        } else {
            gravity.x = acceleration[0].toDouble()
            gravity.y = acceleration[1].toDouble()
            gravity.z = acceleration[2].toDouble()
        }
    }

    private val averageTimeDifferenceInSeconds: Float
        get() = if (count == 0L) {
            startTimeNanos = SystemClock.elapsedRealtimeNanos()
            count++
            0f
        } else {
            val averageNanos = (SystemClock.elapsedRealtimeNanos() - startTimeNanos) / count++
            averageNanos / NANOS_PER_SECOND
        }

    companion object {
        private const val TIME_CONSTANT = 0.2f
        private const val NANOS_PER_SECOND = 1000000000f
    }
}

