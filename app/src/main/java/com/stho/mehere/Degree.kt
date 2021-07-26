package com.stho.mehere

import kotlin.math.IEEErem

class Degree {
    companion object {

        fun sin(degree: Double): Double {
            return kotlin.math.sin(Math.toRadians(degree))
        }

        fun tan(degree: Double): Double {
            return kotlin.math.tan(Math.toRadians(degree))
        }

        fun cos(degree: Double): Double {
            return kotlin.math.cos(Math.toRadians(degree))
        }

        fun arcTan2(y: Double, x: Double): Double {
            return Math.toDegrees(kotlin.math.atan2(y, x))
        }

        fun arcTan(x: Double): Double {
            return Math.toDegrees(kotlin.math.atan(x))
        }

        fun arcSin(x: Double): Double {
            return Math.toDegrees(kotlin.math.asin(x))
        }

        fun arcCos(x: Double): Double {
            return Math.toDegrees(kotlin.math.acos(x))
        }

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

        /**
         * difference x - y of two angles x and y in degree from -180° to 180°
         */
        fun difference(x: Double, y: Double): Double =
            normalizeTo180(x - y)
    }
}


