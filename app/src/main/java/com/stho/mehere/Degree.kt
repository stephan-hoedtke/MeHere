package com.stho.mehere

import kotlin.math.IEEErem
import kotlin.math.PI

class Degree {
    companion object {
        fun getAngleDifference(x: Double, y: Double): Double =
            normalizeTo180(x - y)

        fun normalize(degree: Double): Double =
            degree.IEEErem(360.0).let {
                when {
                    it < 0 -> it + 360.0
                    else -> it
                }
            }

        fun normalizeTo180(degree: Double): Double =
            degree.IEEErem(360.0).let {
                when {
                    it > 180 -> it - 360.0
                    it < -180 -> it + 360.0
                    else -> it
                }
            }

        fun fromRadian(radian: Double): Double =
            radian * RADIANT_TO_DEGREE

        private const val RADIANT_TO_DEGREE: Double = 180.0 / PI
    }
}
