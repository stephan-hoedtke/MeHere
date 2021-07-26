package com.stho.mehere


import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt


/**
 * GEO-Location with latitude in degrees, longitude in degrees, altitude in km
 *
 * Define a new GEO-location.
 * @param latitude in degrees
 * @param longitude in degrees
 * @param altitude in km
 */
class Location(val latitude: Double, val longitude: Double, val altitude: Double = 0.0) {

    override fun toString(): String =
        "$latitude, $longitude"

    internal fun toGeoPoint(): GeoPoint =
        GeoPoint(latitude, longitude, altitude)

    fun isNearTo(otherLocation: Location): Boolean =
        getVerticalDistanceInKmTo(altitude, otherLocation.altitude) < TEN_METERS_IN_KM
                &&
        getEstimatedHorizontalDistanceInKmTo(latitude, longitude, otherLocation.latitude, otherLocation.longitude) < TEN_METERS_IN_KM

    internal fun isSomewhereElse(otherLocation: Location): Boolean =
        getVerticalDistanceInKmTo(altitude, otherLocation.altitude) > TEN_METERS_IN_KM
                ||
        getEstimatedHorizontalDistanceInKmTo(latitude, longitude, otherLocation.latitude, otherLocation.longitude) > TEN_METERS_IN_KM

    internal fun isSomewhereElse(point: IGeoPoint): Boolean =
        getEstimatedHorizontalDistanceInKmTo(latitude, longitude, point.latitude, point.longitude) > TEN_METERS_IN_KM


    companion object {

        private const val TEN_METERS_IN_KM = 0.01
        private const val EARTH_RADIUS_IN_KM = 6378.137000

         fun getEstimatedHorizontalDistanceInKmTo(latitude: Double, longitude: Double, otherLatitude: Double, otherLongitude: Double): Double {
            val dy = Math.toRadians(otherLatitude - latitude)
            val dx = cos(Math.toRadians(latitude)) * Math.toRadians(otherLongitude - longitude)
            val d = sqrt(dx * dx + dy * dy)
            return EARTH_RADIUS_IN_KM * d
        }

        fun getVerticalDistanceInKmTo(altitude: Double, otherAltitude: Double): Double =
            abs(otherAltitude - altitude)

        internal fun fromAndroidLocation(location: android.location.Location): Location =
            Location(location.latitude, location.longitude, location.altitudeInKm())

        internal fun fromGeoPoint(geoPoint: GeoPoint): Location =
            Location(geoPoint.latitude, geoPoint.longitude, geoPoint.altitude)

        val default: Location =
            Location(0.0,0.0)
    }
}

/*
    convert android location altitude from m into km
 */
fun android.location.Location.altitudeInKm(): Double {
    return 0.001 * altitude
}


