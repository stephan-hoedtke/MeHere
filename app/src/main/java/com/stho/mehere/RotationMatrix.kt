package com.stho.mehere


data class RotationMatrix (
    val m11: Double,
    val m12: Double,
    val m13: Double,
    val m21: Double,
    val m22: Double,
    val m23: Double,
    val m31: Double,
    val m32: Double,
    val m33: Double) {

    companion object {

        fun fromFloatArray(m: FloatArray) =
            RotationMatrix(
                m11 = m[0].toDouble(),
                m12 = m[1].toDouble(),
                m13 = m[2].toDouble(),
                m21 = m[3].toDouble(),
                m22 = m[4].toDouble(),
                m23 = m[5].toDouble(),
                m31 = m[6].toDouble(),
                m32 = m[7].toDouble(),
                m33 = m[8].toDouble(),
            )
    }
}
