package com.stho.mehere

import android.os.SystemClock

internal class Acceleration {
    // s(t) = a * t^3 + b * t^2 + c * t + d
    // s(0) = s0
    // s(1) = s1
    // s'(0) = v0
    // s'(1) = 0
    // -->  a = v0 - 2 (s1 - s0)
    //      b = 3 (s1 - s0) - 2 v0
    //      c = v0
    //      d = s0
    private var v0: Double
    private var s0: Double
    private var s1: Double
    private var a = 0.0
    private var b = 0.0
    private var c = 0.0
    private var d = 0.0
    private var t0: Long
    private val factor: Double

    val position: Double
        get() {
            val t = getTime(SystemClock.elapsedRealtimeNanos())
            return getPosition(t)
        }

    fun update(targetPosition: Double) {
        val elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        val t = getTime(elapsedRealtimeNanos)
        val v = getSpeed(t)
        val s = getPosition(t)
        v0 = v
        s0 = s
        s1 = targetPosition
        t0 = elapsedRealtimeNanos
        calculateFormula()
    }

    fun rotateTo(targetPosition: Double) {
        val elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        val t = getTime(elapsedRealtimeNanos)
        val v = getSpeed(t)
        val s = getPosition(t)
        val alpha = Degree.getAngleDifference(targetPosition, s)
        v0 = v
        s0 = s
        s1 = s + alpha
        t0 = elapsedRealtimeNanos

        when {
            alpha > 0 && s1 > 360 -> {
                s0 -= 360
                s1 -= 360
            }
            alpha < 0 && s1 < 0 -> {
                s0 += 360
                s1 += 360
            }
        }

        calculateFormula()
    }

    private fun getTime(elapsedRealtimeNanos: Long): Double {
        val nanos = elapsedRealtimeNanos - t0
        return factor * nanos
    }

    private fun calculateFormula() {
        a = v0 - 2 * (s1 - s0)
        b = 3 * (s1 - s0) - 2 * v0
        c = v0
        d = s0
    }

    private fun getPosition(t: Double): Double {
        return when {
            t < 0 -> s0
            t > 1 -> s1
            else -> ((a * t + b) * t + c) * t + d
        }
    }

    private fun getSpeed(t: Double): Double {
        return when {
            t < 0 -> v0
            t > 1 -> 0.0
            else -> (3 * a * t + 2 * b) * t + c
        }
    }

    companion object {
        private const val NANOSECONDS_PER_SECOND = 1000000000.0
    }

    init {
        factor = 1.1 / NANOSECONDS_PER_SECOND
        v0 = 0.0
        s0 = 0.0
        s1 = 0.0
        t0 = SystemClock.elapsedRealtimeNanos()
        calculateFormula()
    }
}