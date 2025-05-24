package com.abhishekdadhich.movemate

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TfNSWApiService {

    companion object {
        const val BASE_URL = "https://api.transport.nsw.gov.au/v1/tp/"
    }

    @GET("trip")
    suspend fun getTrip(
        @Header("Authorization") apiKey: String,
        @Query("outputFormat") outputFormat: String = "rapidJSON",
        @Query("coordOutputFormat") coordOutputFormat: String = "EPSG:4326",
        @Query("depArrMacro") depArrMacro: String,
        @Query("itdDate") itdDate: String,
        @Query("itdTime") itdTime: String,
        @Query("type_origin") typeOrigin: String,
        @Query("name_origin") nameOrigin: String,
        @Query("type_destination") typeDestination: String,
        @Query("name_destination") nameDestination: String,
        @Query("calcNumberOfTrips") calcNumberOfTrips: Int = 8,
        @Query("TfNSWTR") tfNSWTR: String = "true",
        @Query("version") version: String = "10.2.1.42" // As per Swagger
    ): Response<TripResponse>

    @GET("stop_finder")
    suspend fun findStops(
        @Header("Authorization") apiKey: String,
        @Query("name_sf") searchTerm: String, // The user's typed text
        @Query("type_sf") typeSf: String = "any", // [cite: 640] "any" allows searching all types
        @Query("outputFormat") outputFormat: String = "rapidJSON", // [cite: 636]
        @Query("coordOutputFormat") coordOutputFormat: String = "EPSG:4326", // [cite: 648]
        @Query("TfNSWSF") tfNSWSF: String = "true", // [cite: 650] Recommended for website-like results
        @Query("version") version: String = "10.2.1.42" // [cite: 652] As per Swagger default
    ): Response<StopFinderResponse>
}