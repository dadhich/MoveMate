package com.abhishekdadhich.movemate

import com.google.gson.annotations.SerializedName

// --- TripResponse and related models ---
data class TripResponse(
    @SerializedName("journeys") val journeys: List<Journey>?,
    @SerializedName("error") val error: ApiError?
)

data class Journey(
    @SerializedName("legs") val legs: List<JourneyLeg>?
    // You might also have interchanges: Int? here if needed from your Logcat
)

data class JourneyLeg(
    @SerializedName("origin") val origin: LegStop?,
    @SerializedName("destination") val destination: LegStop?,
    @SerializedName("transportation") val transportation: Transportation?,
    @SerializedName("duration") val duration: Int?, // Duration in seconds
    @SerializedName("infos") val infos: List<LegStopInfo>?,
    @SerializedName("stopSequence") val stopSequence: List<LegStop>? // ADDED THIS FIELD
    // Add other leg details like coords, footPathInfo, etc. as needed from Swagger
)

data class LegStop(
    @SerializedName("name") val name: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("departureTimePlanned") val departureTimePlanned: String?,
    @SerializedName("arrivalTimePlanned") val arrivalTimePlanned: String?,
    @SerializedName("departureTimeEstimated") val departureTimeEstimated: String?,
    @SerializedName("arrivalTimeEstimated") val arrivalTimeEstimated: String?,
    @SerializedName("type") val type: String?, // e.g., "platform", "stop"
    @SerializedName("parent") val parent: ParentLocation? // Added for completeness
    // Add other LegStop properties like properties.WheelchairAccess if needed
)

data class Transportation(
    @SerializedName("name") val name: String?,
    @SerializedName("number") val number: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("product") val product: Product?,
    @SerializedName("operator") val operator: Operator?,
    @SerializedName("destination") val transportDestination: TransportDestination?
    // Add properties like RealtimeTripId if needed
)

data class Product(
    @SerializedName("name") val name: String?, // e.g., "Train", "Bus"
    @SerializedName("class") val classId: Int? // e.g., 1=Train, 5=Bus
)

data class Operator(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class TransportDestination(
    @SerializedName("name") val name: String?
)

data class LegStopInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("content") val content: String?
)

data class ApiError(
    @SerializedName("message") val message: String?
)

// --- StopFinder API response models ---
data class StopFinderResponse(
    @SerializedName("version") val version: String?,
    @SerializedName("error") val error: ApiError?,
    @SerializedName("locations") val locations: List<StopFinderLocation>?
)

data class StopFinderLocation(
    @SerializedName("id") val id: String?,
    @SerializedName("isGlobalId") val isGlobalId: Boolean?,
    @SerializedName("name") val name: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("coord") val coordinates: List<Double>?,
    @SerializedName("matchQuality") val matchQuality: Int?,
    @SerializedName("isBest") val isBest: Boolean?,
    @SerializedName("parent") val parent: ParentLocation?,
    @SerializedName("modes") val modes: List<Int>?
)

data class ParentLocation(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("type") val type: String?
)

// --- App-specific Route model (used by RouteAdapter on MainActivity) ---
enum class RouteStatusType {
    ON_TIME, DELAYED, EARLY
}

data class TransportTag(
    val iconResId: Int,
    val text: String
)

data class Route(
    val id: String,
    val routeName: String,
    val firstVehicleActualDepartureUTC: Long?,
    val firstVehicleScheduledDepartureUTC: Long?,
    val firstVehicleEstimatedDepartureUTC: Long?,
    val overallJourneyETAForDisplay: String,
    val overallJourneyDepartureTimeForDisplay: String,
    val transfersCount: Int,
    val transportTags: List<TransportTag>
)