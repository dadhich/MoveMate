package com.abhishekdadhich.movemate

import com.google.gson.annotations.SerializedName

// Main response structure for /trip endpoint
data class TripResponse(
    @SerializedName("journeys") val journeys: List<Journey>?,
    @SerializedName("error") val error: ApiError?
)

data class Journey(
    @SerializedName("legs") val legs: List<JourneyLeg>?
)

data class JourneyLeg(
    @SerializedName("origin") val origin: LegStop?,
    @SerializedName("destination") val destination: LegStop?,
    @SerializedName("transportation") val transportation: Transportation?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("infos") val infos: List<LegStopInfo>?
)

data class LegStop(
    @SerializedName("name") val name: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("departureTimePlanned") val departureTimePlanned: String?,
    @SerializedName("arrivalTimePlanned") val arrivalTimePlanned: String?,
    @SerializedName("departureTimeEstimated") val departureTimeEstimated: String?,
    @SerializedName("arrivalTimeEstimated") val arrivalTimeEstimated: String?
)

data class Transportation(
    @SerializedName("name") val name: String?,
    @SerializedName("number") val number: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("product") val product: Product?,
    @SerializedName("operator") val operator: Operator?,
    @SerializedName("destination") val transportDestination: TransportDestination?
)

data class Product(
    @SerializedName("name") val name: String?,
    @SerializedName("class") val classId: Int?
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

data class StopFinderResponse(
    @SerializedName("version") val version: String?,
    @SerializedName("error") val error: ApiError?, // Reusing ApiError from TripResponse
    @SerializedName("locations") val locations: List<StopFinderLocation>?
)

data class StopFinderLocation(
    @SerializedName("id") val id: String?, // Crucial for using as origin/destination ID
    @SerializedName("isGlobalId") val isGlobalId: Boolean?,
    @SerializedName("name") val name: String?, // Long name, may include suburb
    @SerializedName("disassembledName") val disassembledName: String?, // Short name
    @SerializedName("type") val type: String?, // e.g., "stop", "platform", "locality", "poi"
    @SerializedName("coord") val coordinates: List<Double>?, // [latitude, longitude] or [longitude, latitude] - API doc says first is lat, then lon for /coord, for /stop_finder this needs checking from response
    @SerializedName("matchQuality") val matchQuality: Int?,
    @SerializedName("isBest") val isBest: Boolean?,
    @SerializedName("parent") val parent: ParentLocation?, // Parent location details
    @SerializedName("modes") val modes: List<Int>? // List of transport mode codes servicing this stop
)

// ParentLocation can be reused or redefined if StopFinder's parent is different
// From the Swagger, ParentLocation seems generic. Let's assume it's simple for now.
// If it's the same as used in TripRequestResponseJourneyLegStop, we can reuse that definition.
// The Swagger provided shows ParentLocation definition once.
data class ParentLocation(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("disassembledName") val disassembledName: String?,
    @SerializedName("type") val type: String?
    // It can also have a nested parent, but we'll keep it simple for now
)