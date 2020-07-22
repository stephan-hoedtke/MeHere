package com.stho.mehere

import kotlin.math.sqrt

/**
 * Created by shoedtke on 20.01.2017.
 */
class Vector {
    var x: Double
    var y: Double
    var z: Double

    constructor() {
        x = 0.0
        y = 0.0
        z = 0.0
    }

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    val length: Double
        get() = sqrt(x * x + y * y + z * z)

    operator fun minus(v: Vector): Vector {
        return Vector(x - v.x, y - v.y, z - v.z)
    }

    val values: FloatArray?
        get() = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())
}

