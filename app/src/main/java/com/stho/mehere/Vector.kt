package com.stho.nyota.sky.utilities

import kotlin.math.sqrt


interface IVector {
    val x: Double
    val y: Double
    val z: Double
    val length: Double
}

/**
 * Created by shoedtke on 20.01.2017.
 */
data class Vector(override val x: Double = 0.0, override val y: Double = 0.0, override val z: Double = 0.0) : IVector {

    val values: FloatArray
        get() = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())

    override val length: Double by lazy { sqrt(x * x + y * y + z * z) }

    operator fun plus(v: Vector): Vector =
        Vector(x + v.x, y + v.y, z + v.z)

    operator fun minus(v: Vector): Vector =
        Vector(x - v.x, y - v.y, z - v.z)

    operator fun times(f: Double): Vector =
        Vector(x * f, y * f, z * f)

    operator fun div(f: Double): Vector =
        Vector(x / f, y / f, z / f)

    fun cross(v: Vector): Vector =
        cross(this, v)

    fun dot(v: Vector): Double =
        dot(this, v)

    companion object {
        val default: Vector =
            Vector(0.0, 0.0, 0.0)

        fun fromFloatArray(v: FloatArray): Vector =
            Vector(
                x = v[0].toDouble(),
                y = v[1].toDouble(),
                z = v[2].toDouble()
            )

        private fun dot(a: Vector, b: Vector): Double =
            a.x * b.x + a.y * b.y + a.z * b.z

        private fun cross(a: Vector, b: Vector): Vector =
            Vector(
                x = a.y * b.z - a.z * b.y,
                y = a.z * b.x - a.x * b.z,
                z = a.x * b.y - a.y * b.x
            )

    }
}

