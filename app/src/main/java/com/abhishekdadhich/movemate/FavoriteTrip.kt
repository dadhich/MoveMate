package com.abhishekdadhich.movemate

import java.io.Serializable

data class FavoriteTrip(
    val id: String,
    val name: String,
    val originName: String,
    val originStopId: String?,
    val originLatitude: Double?,
    val originLongitude: Double?,
    val destinationName: String,
    val destinationStopId: String
) : Serializable