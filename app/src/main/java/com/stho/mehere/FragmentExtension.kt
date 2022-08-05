package com.stho.mehere;

import androidx.fragment.app.Fragment
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun Fragment.formatZoom(zoom: Double): String =
    resources.getString(R.string.format_zoom, zoom)

internal fun Fragment.formatLatitude(location: Location): String =
    resources.getString(R.string.format_latitude, location.latitude)

internal fun Fragment.formatLongitude(location: Location): String =
    resources.getString(R.string.format_longitude, location.longitude)

internal fun Fragment.formatAltitude(location: Location): String =
    resources.getString(R.string.format_altitude, location.altitude)

internal fun Fragment.formatDateTime(date: ZonedDateTime): String {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return date.format(formatter)
}

