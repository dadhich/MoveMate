package com.abhishekdadhich.movemate // Make sure this matches your package name

// Enum to represent the status indicator type/color
//enum class RouteStatusType {
//    ON_TIME, DELAYED, EARLY
//}
//
//// Data class for individual transport legs/tags within a route
//data class TransportTag(
//    val iconResId: Int,
//    val text: String
//)
//
//// Main data class for a Route
//data class Route(
//    val id: String,
//    val routeName: String,
//
//    // For "In X min" - departure of the first *vehicle*
//    val firstVehicleActualDepartureUTC: Long?, // Estimated if available, else Planned
//
//    // For status ("- Y min early/delayed/on time") - based on the first *vehicle*
//    val firstVehicleScheduledDepartureUTC: Long?,
//    val firstVehicleEstimatedDepartureUTC: Long?,
//
//    // Overall journey arrival time for basic display if needed
//    val overallJourneyETAForDisplay: String,
//
//    // Overall journey first leg departure for basic display
//    val overallJourneyDepartureTimeForDisplay: String,
//
//    val transfersCount: Int,
//    val transportTags: List<TransportTag>
//)