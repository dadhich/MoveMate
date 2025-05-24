package com.abhishekdadhich.movemate

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var journeyLegAdapter: JourneyLegAdapter
    private lateinit var recyclerViewJourneyLegs: RecyclerView

    // Formatters (can be moved to a companion object or utils if used elsewhere)
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_details)

        // Set status bar color to match the dark theme of the header
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_transparent)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish() // Go back to the previous activity
        }

        recyclerViewJourneyLegs = findViewById(R.id.recyclerViewJourneyLegs)
        recyclerViewJourneyLegs.layoutManager = LinearLayoutManager(this)
        // Initialize adapter with an empty list, it will be updated later
        journeyLegAdapter = JourneyLegAdapter(emptyList())
        recyclerViewJourneyLegs.adapter = journeyLegAdapter

        val journeyJson = intent.getStringExtra("SELECTED_JOURNEY_JSON")

        if (journeyJson != null) {
            try {
                val journey = Gson().fromJson(journeyJson, Journey::class.java)
                if (journey != null) {
                    populateHeader(journey)
                    journey.legs?.let {
                        journeyLegAdapter.updateLegs(it)
                    }
                } else {
                    handleDataError("Failed to parse journey details.")
                }
            } catch (e: Exception) {
                Log.e("TripDetailsActivity", "Error parsing Journey JSON", e)
                handleDataError("Error reading trip details.")
            }
        } else {
            handleDataError("No trip data received.")
        }
    }

    private fun parseAndFormatTimeDisplay(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "N/A"
        return try {
            apiDateTimeFormat.parse(dateTimeString)?.let { displayTimeFormat.format(it) } ?: "N/A"
        } catch (e: ParseException) {
            "N/A"
        }
    }

    private fun populateHeader(journey: Journey) {
        val textTotalTime = findViewById<TextView>(R.id.textTotalTime)
        val textOverallDepartureTime = findViewById<TextView>(R.id.textOverallDepartureTime)
        val textOverallArrivalTime = findViewById<TextView>(R.id.textOverallArrivalTime)

        var totalDurationSeconds = 0
        journey.legs?.forEach { leg ->
            totalDurationSeconds += leg.duration ?: 0
        }
        val totalDurationMinutes = TimeUnit.SECONDS.toMinutes(totalDurationSeconds.toLong())
        textTotalTime.text = if (totalDurationMinutes > 0) "$totalDurationMinutes min" else "N/A"

        val firstLegDeparture = journey.legs?.firstOrNull()?.origin?.let {
            parseAndFormatTimeDisplay(it.departureTimeEstimated ?: it.departureTimePlanned)
        } ?: "N/A"
        textOverallDepartureTime.text = firstLegDeparture

        val lastLegArrival = journey.legs?.lastOrNull()?.destination?.let {
            parseAndFormatTimeDisplay(it.arrivalTimeEstimated ?: it.arrivalTimePlanned)
        } ?: "N/A"
        textOverallArrivalTime.text = lastLegArrival
    }

    private fun handleDataError(message: String) {
        Log.e("TripDetailsActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish() // Close activity if data is problematic
    }
}