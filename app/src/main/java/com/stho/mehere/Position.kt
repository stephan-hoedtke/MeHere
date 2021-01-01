package com.stho.mehere

import android.location.Location
import android.location.LocationManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.api.IGeoPoint
import kotlin.math.abs

class Position internal constructor(val latitude: Double, val longitude: Double, val altitude: Double) {

    constructor(location: Location) : this(location.latitude, location.longitude, location.altitude)

    internal fun toGeoPoint(): GeoPoint =
        GeoPoint(latitude, longitude, altitude)

    internal fun isSomewhereElse(position: Position): Boolean =
        latitude.isDifferentFrom(position.latitude) || longitude.isDifferentFrom(position.longitude) || altitude.isDifferentFrom(position.altitude)

    internal fun isSomewhereElse(point: IGeoPoint): Boolean =
        latitude.isDifferentFrom(point.latitude) || longitude.isDifferentFrom(point.longitude)
}

private fun Double.isDifferentFrom(x: Double): Boolean =
    abs(x - this) > 0.000000000001