package com.stho.mehere;


import java.time.ZonedDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PointOfInterest(
    val id: String,
    val location: Location,
    val name: String,
    val description: String,
    val type: Type = Type.TRACK,
    @Serializable(ZonedDateTimeSerializer::class)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    @Serializable(ZonedDateTimeSerializer::class)
    val lastModifiedAt: ZonedDateTime)
{
    enum class Type private constructor(private val friendlyName: String) {
        START("Starting Point"),
        TRACK("Tracking"),
        SIGHT("Interesting Sight");

        override fun toString(): String {
            return friendlyName
        }

        companion object {
            fun parseString(str: String): Type =
                values().first { it.toString() == str }
        }
    }

    companion object {
        internal const val PREFIX = "poi"
        internal const val MINUS = " - "

        internal fun createNewId(dateTime: ZonedDateTime): String =
            PointOfInterest.PREFIX + dateTime.toInstant().toEpochMilli().toString()

        internal fun update(point: PointOfInterest, name: String, description: String, type: PointOfInterest.Type): PointOfInterest =
            PointOfInterest(
                id = point.id,
                location = point.location,
                name = name,
                description = description,
                type = type,
                createdAt = point.createdAt,
                lastModifiedAt = ZonedDateTime.now())

        internal fun create(location: Location, name: String, description: String, type: Type): PointOfInterest {
            val now = ZonedDateTime.now()
            return PointOfInterest(
                    id = PointOfInterest.createNewId(now),
                    location = location,
                    name = name,
                    description = description,
                    type = type,
                    createdAt = now,
                    lastModifiedAt = now
                )
            }
    }
}

