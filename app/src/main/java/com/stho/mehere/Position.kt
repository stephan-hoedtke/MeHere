package com.stho.mehere

import android.location.Location
import android.location.LocationManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.api.IGeoPoint
import kotlin.math.abs

class Position internal constructor(val latitude: Double, val longitude: Double, val altitude: Double) {

    constructor(location: Location) : this(location.latitude, location.longitude, location.altitude)

    internal fun toLocation(): Location {
        return Location(LocationManager.GPS_PROVIDER).also {
            it.latitude = latitude
            it.longitude = longitude
            it.altitude = altitude
        }
    }

    internal fun toGeoPoint(): GeoPoint {
        return GeoPoint(latitude, longitude, altitude)
    }

    internal fun isSomewhereElse(position: Position): Boolean {
        return isSomewhereElse(position.latitude, position.longitude, position.altitude)
    }

    internal fun isSomewhereElse(location: Location): Boolean {
        return isSomewhereElse(location.latitude, location.longitude, location.altitude)
    }

    internal fun isSomewhereElse(point: IGeoPoint): Boolean {
        return isSomewhereElse(point.latitude, point.longitude)
    }

    private fun isSomewhereElse(latitude: Double, longitude: Double, altitude: Double): Boolean {
        return isDifferent(this.latitude, latitude) || isDifferent(this.longitude, longitude) || isDifferent(this.altitude, altitude)
    }

    private fun isSomewhereElse(latitude: Double, longitude: Double): Boolean {
        return isDifferent(this.latitude, latitude) || isDifferent(this.longitude, longitude)
    }

    private fun isDifferent(a: Double, b: Double): Boolean {
        return abs(a - b) > 0.0000000001
    }
}