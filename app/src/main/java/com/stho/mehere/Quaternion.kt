package com.stho.nyota.sky.utilities

import com.stho.mehere.RotationMatrix
import kotlin.math.*

/**
 * https://mathworld.wolfram.com/Quaternion.html
 * https://www.ashwinnarayan.com/post/how-to-integrate-quaternions/
 */
data class Quaternion(val v: Vector, val s: Double) {
    val x: Double = v.x
    val y: Double = v.y
    val z: Double = v.z

    fun toRotationMatrix(): RotationMatrix {
        // https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
        val x2 = 2 * x * x
        val y2 = 2 * y * y
        val z2 = 2 * z * z
        val xy = 2 * x * y
        val xz = 2 * x * z
        val yz = 2 * y * z
        val sz = 2 * s * z
        val sy = 2 * s * y
        val sx = 2 * s * x
        return RotationMatrix(
            1 - y2 - z2,
            xy - sz,
            xz + sy,
            xy + sz,
            1 - x2 - z2,
            yz - sx,
            xz - sy,
            yz + sx,
            1 - x2 - y2
        )
    }

    fun toOrientation(): Orientation =
        Rotation.getOrientationFor(toRotationMatrix())

    constructor(x: Double, y: Double, z: Double, s: Double) :
            this(v = Vector(x, y, z), s = s)

    operator fun plus(q: Quaternion): Quaternion =
            Quaternion(v + q.v, s + q.s)

    operator fun minus(q: Quaternion): Quaternion =
            Quaternion(v - q.v, s - q.s)

    operator fun times(f: Double): Quaternion =
            Quaternion(v * f, s * f)

    operator fun times(q: Quaternion): Quaternion =
            hamiltonProduct(this, q)

    operator fun div(f: Double): Quaternion =
            Quaternion(v / f, s / f)

    private fun norm(): Double =
            sqrt(normSquare())

    private fun normSquare(): Double =
            x * x + y * y + z * z + s * s

    fun conjugate(): Quaternion =
            Quaternion(Vector(-x, -y, -z), s)

    fun inverse(): Quaternion =
        conjugate() * (1 / normSquare())

    fun normalize(): Quaternion =
        this * (1.0 / norm())

    companion object {

        /**
         * Quaternion for rotating by theta (in radians) around the vector u
         */
        fun forRotation(u: Vector, theta: Double): Quaternion {
            // see: https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation
            val thetaOverTwo = theta / 2.0
            val sinThetaOverTwo: Double = sin(thetaOverTwo)
            val cosThetaOverTwo: Double = cos(thetaOverTwo)
            return Quaternion(v = u * sinThetaOverTwo, s = cosThetaOverTwo)
        }

        val default: Quaternion
            get() = Quaternion(0.0, 0.0, 0.0, 1.0)

        // (r1,v1) * (r2,v2) = (r1 r2 - dot(v1,v2), r1 v2 + r2 v1 + cross(v1, v2)
        private fun hamiltonProduct(a: Quaternion, b: Quaternion): Quaternion {
            val a1 = a.s
            val b1 = a.x
            val c1 = a.y
            val d1 = a.z
            val a2 = b.s
            val b2 = b.x
            val c2 = b.y
            val d2 = b.z
            return Quaternion(
                x = a1 * b2 + b1 * a2 + c1 * d2 - d1 * c2,
                y = a1 * c2 + c1 * a2 - b1 * d2 + d1 * b2,
                z = a1 * d2 + d1 * a2 + b1 * c2 - c1 * b2,
                s = a1 * a2 - b1 * b2 - c1 * c2 - d1 * d2
            )
        }

        fun fromRotationMatrix(m: RotationMatrix): Quaternion {
            // see: https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
            // mind, as both q and -q define the same rotation you may get q or -q, respectively
            m.run {
                when {
                    m11 + m22 + m33 > 0 -> {
                        val fourS = 2.0 * sqrt(1.0 + m11 + m22 + m33) // 4s = 4 * q.s
                        return Quaternion(
                            x = (m32 - m23) / fourS,
                            y = (m13 - m31) / fourS,
                            z = (m21 - m12) / fourS,
                            s = 0.25 * fourS
                        )
                    }
                    m11 > m22 && m11 > m33 -> {
                        val fourX = 2.0 * sqrt(1.0 + m11 - m22 - m33) // 4x = 4 * q.x
                        return Quaternion(
                            x = 0.25 * fourX,
                            y = (m12 + m21) / fourX,
                            z = (m13 + m31) / fourX,
                            s = (m32 - m23) / fourX,
                        )
                    }
                    m22 > m33 -> {
                        val fourY = 2.0 * sqrt(1.0 + m22 - m11 - m33) // 4y = 4*q.y
                        return Quaternion(
                            x = (m12 + m21) / fourY,
                            y = 0.25 * fourY,
                            z = (m23 + m32) / fourY,
                            s = (m13 - m31) / fourY
                        )
                    }
                    else -> {
                        val fourZ = 2.0 * sqrt(1.0 + m33 - m11 - m22) // 4z = 4 * q.z
                        return Quaternion(
                            x = (m13 + m31) / fourZ,
                            y = (m23 + m32) / fourZ,
                            z = 0.25 * fourZ,
                            s = (m21 - m12) / fourZ
                        )
                    }
                }
            }
        }

        private fun dot(a: Quaternion, b: Quaternion): Double =
            a.x * b.x + a.y * b.y + a.z * b.z + a.s * b.s

        private const val COS_THETA_THRESHOLD: Double = 0.9995

        /**
         * Q(t) := A sin((1 - t) * θ) / sin(θ) + B sin(t * θ) / sin(θ)
         *      with cos(θ) = dot(A, B)
         *
         *      To ensure -90 <= θ <= 90: use -A when dot(A,B) < 0
         *      Note:
         *          Q(0) = A
         *          Q(1) = B
         */
        fun interpolate(a: Quaternion, b: Quaternion, t: Double): Quaternion {
            // see: https://theory.org/software/qfa/writeup/node12.html
            // see: https://blog.magnum.graphics/backstage/the-unnecessarily-short-ways-to-do-a-quaternion-slerp/

            val cosTheta = dot(a, b)

            return when {
                abs(cosTheta) > COS_THETA_THRESHOLD -> {
                    // If the inputs are too close for comfort, linearly interpolate and normalize the result.
                    val c: Quaternion = a + (a - b) * t
                    return c.normalize()
                }
                cosTheta >= 0 -> {
                    val theta: Double = acos(cosTheta)
                    val sinTheta = sin(theta)
                    val f1 = sin((1 - t) * theta) / sinTheta
                    val f2 = sin(t * theta) / sinTheta
                    a * f1 + b * f2
                }
                else -> {
                    // Use the shorter way for -a ...
                    val theta: Double = acos(-cosTheta)
                    val sinTheta = sin(theta)
                    val f1 = sin((t - 1) * theta) / sinTheta
                    val f2 = sin(t * theta) / sinTheta
                    a * f1 + b * f2
                }
            }
        }
    }
}




