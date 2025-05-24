package com.abhishekdadhich.movemate

import android.util.Log // ADDED THIS IMPORT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip // Ensure Chip is imported
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class JourneyLegAdapter(private var legs: List<JourneyLeg>) :
    RecyclerView.Adapter<JourneyLegAdapter.LegViewHolder>() {

    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private fun parseAndFormatTime(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return "N/A"
        return try {
            val date = apiDateTimeFormat.parse(dateTimeString)
            if (date != null) displayTimeFormat.format(date) else "N/A"
        } catch (e: ParseException) {
            Log.e("JourneyLegAdapter", "Error parsing date: $dateTimeString", e)
            "N/A"
        }
    }

    class LegViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val legIcon: ImageView = itemView.findViewById(R.id.legIcon)
        val legModeName: TextView = itemView.findViewById(R.id.legModeName)
        val legDurationChip: Chip = itemView.findViewById(R.id.legDurationChip)
        val textFromLocation: TextView = itemView.findViewById(R.id.textFromLocation)
        val textToLocation: TextView = itemView.findViewById(R.id.textToLocation)
        val textDepartureTime: TextView = itemView.findViewById(R.id.textDepartureTime)
        val textArrivalTime: TextView = itemView.findViewById(R.id.textArrivalTime)
        val labelStops: TextView = itemView.findViewById(R.id.labelStops)
        val textStopsValue: TextView = itemView.findViewById(R.id.textStopsValue)
        val labelStandPlatform: TextView = itemView.findViewById(R.id.labelStandPlatform)
        val textStandPlatformValue: TextView = itemView.findViewById(R.id.textStandPlatformValue)
        val textInstruction: TextView = itemView.findViewById(R.id.textInstruction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journey_leg, parent, false)
        return LegViewHolder(view)
    }

    override fun onBindViewHolder(holder: LegViewHolder, position: Int) {
        val leg = legs[position]
        // val context = holder.itemView.context // Not explicitly needed if not using ContextCompat

        var legModeDisplayName = "Unknown Mode"
        var legIconRes = R.drawable.ic_walk_24dp

        leg.transportation?.product?.let { product ->
            legModeDisplayName = when (product.classId) {
                1 -> "Train ${leg.transportation.number ?: ""}".trim()
                2 -> "Metro ${leg.transportation.number ?: ""}".trim()
                4 -> "Light Rail ${leg.transportation.number ?: ""}".trim()
                5 -> "Bus ${leg.transportation.number ?: ""}".trim()
                7 -> "Coach ${leg.transportation.number ?: ""}".trim()
                9 -> "Ferry ${leg.transportation.number ?: ""}".trim()
                11 -> "School Bus ${leg.transportation.number ?: ""}".trim()
                99, 100 -> "Walking"
                else -> product.name ?: leg.transportation.name ?: "Vehicle"
            }
            legIconRes = when (product.classId) {
                1, 4 -> R.drawable.ic_train_alt_16dp
                2 -> R.drawable.ic_metro_24dp
                5, 11 -> R.drawable.ic_bus_alt_16dp
                7 -> R.drawable.ic_coach_24dp
                9 -> R.drawable.ic_directions_boat_24dp
                99, 100 -> R.drawable.ic_walk_24dp
                else -> R.drawable.ic_walk_24dp // Default icon for unknown
            }
        }
        if (leg.transportation == null && (leg.duration != null && leg.duration > 0)) {
            if (leg.transportation?.product == null) { // More specific check for walk
                legModeDisplayName = "Walking"
                legIconRes = R.drawable.ic_walk_24dp
            }
        }

        holder.legIcon.setImageResource(legIconRes)
        holder.legModeName.text = legModeDisplayName

        holder.legDurationChip.text =
            leg.duration?.let { "${TimeUnit.SECONDS.toMinutes(it.toLong())} min" } ?: ""
        holder.legDurationChip.visibility =
            if (leg.duration != null && leg.duration > 0) View.VISIBLE else View.GONE

        holder.textFromLocation.text = leg.origin?.disassembledName ?: leg.origin?.name ?: "N/A"
        holder.textToLocation.text =
            leg.destination?.disassembledName ?: leg.destination?.name ?: "N/A"

        holder.textDepartureTime.text = parseAndFormatTime(
            leg.origin?.departureTimeEstimated ?: leg.origin?.departureTimePlanned
        )
        holder.textArrivalTime.text = parseAndFormatTime(
            leg.destination?.arrivalTimeEstimated ?: leg.destination?.arrivalTimePlanned
        )

        // Calculate intermediate stops: total stops in sequence - 2 (origin & destination)
        // Only makes sense for vehicle legs.
        val isVehicleLeg = leg.transportation?.product?.classId !in listOf(99, 100)
        val stopCount = if (isVehicleLeg) (leg.stopSequence?.size ?: 0) - 2 else -1

        if (isVehicleLeg && stopCount >= 0) { // Show if 0 or more intermediate stops
            holder.labelStops.visibility = View.VISIBLE
            holder.textStopsValue.visibility = View.VISIBLE
            holder.textStopsValue.text = stopCount.toString()
        } else {
            holder.labelStops.visibility = View.GONE
            holder.textStopsValue.visibility = View.GONE
        }

        // Platform/Stand Info - trying to get from origin.parent.properties.platform or similar based on mockup
        // This requires more detailed knowledge of your specific API structure for platform/stand
        // For now, using a simplified placeholder logic
        var platformInfoString: String? = null
        var platformLabel = "Platform" // Default label

        // Attempt to get platform/stand from origin's properties if available (this is a guess)
        // The exact path in your TfNSWModels.kt might be different, e.g. leg.origin.properties.platform
        // From your log, it seems like leg.origin.platform (for platform ID "SDT1", "CE21") might be there,
        // or leg.origin.parent.properties.platform for the stop-level platform.
        // The mockup has "Platform 16", "Stand C".
        // Let's assume `leg.origin.name` or `leg.origin.disassembledName` might contain it like "Platform X" or "Stand Y".
        val originName = leg.origin?.name ?: ""
        val originDisassembledName = leg.origin?.disassembledName ?: ""

        if (originName.contains("Platform ", ignoreCase = true)) {
            platformInfoString = originName.substringAfterLast("Platform ").trim()
                .split(",")[0] // Get "16" from "Platform 16, Sydney"
            platformLabel = "Platform"
        } else if (originName.contains("Stand ", ignoreCase = true)) {
            platformInfoString = originName.substringAfterLast("Stand ").trim().split(",")[0]
            platformLabel = "Stand"
        } else if (originDisassembledName.contains("Platform ", ignoreCase = true)) {
            platformInfoString =
                originDisassembledName.substringAfterLast("Platform ").trim().split(",")[0]
            platformLabel = "Platform"
        } else if (originDisassembledName.contains("Stand ", ignoreCase = true)) {
            platformInfoString =
                originDisassembledName.substringAfterLast("Stand ").trim().split(",")[0]
            platformLabel = "Stand"
        }


        if (!platformInfoString.isNullOrBlank() && isVehicleLeg) {
            holder.labelStandPlatform.text = platformLabel
            holder.textStandPlatformValue.text = platformInfoString
            holder.labelStandPlatform.visibility = View.VISIBLE
            holder.textStandPlatformValue.visibility = View.VISIBLE
        } else {
            holder.labelStandPlatform.visibility = View.GONE
            holder.textStandPlatformValue.visibility = View.GONE
        }

        var instruction = ""
        // ... (instruction logic remains similar to previous version, refine as needed)
        if (leg.transportation?.product?.classId == 99 || leg.transportation?.product?.classId == 100) {
            instruction = "Continue walking to next stop"
        } else if (position < legs.size - 1) {
            val nextLeg = legs.getOrNull(position + 1)
            val nextModeClass = nextLeg?.transportation?.product?.classId
            val nextModeName = when (nextModeClass) {
                1, 2, 4 -> "train"
                5, 7, 11 -> "bus"
                9 -> "ferry"
                99, 100 -> "walk"
                else -> "next service"
            }
            instruction = "Exit and proceed to $nextModeName"
        } else {
            instruction = "Exit and proceed to exit"
        }
        holder.textInstruction.text = instruction
        holder.textInstruction.visibility =
            if (instruction.isNotBlank()) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = legs.size

    fun updateLegs(newLegs: List<JourneyLeg>) {
        legs = newLegs
        notifyDataSetChanged()
    }
}