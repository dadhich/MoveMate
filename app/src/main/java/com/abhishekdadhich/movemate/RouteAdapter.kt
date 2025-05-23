package com.abhishekdadhich.movemate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RouteAdapter(private var routes: List<Route>) :
    RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val statusDot: ImageView = itemView.findViewById(R.id.imageViewStatusDot) // Removed
        val routeName: TextView = itemView.findViewById(R.id.textViewRouteName)
        val durationAndStatusText: TextView = itemView.findViewById(R.id.textViewRouteDuration)
        val oldStatusText: TextView = itemView.findViewById(R.id.textViewStatus) // Should be GONE

        val departureTime: TextView = itemView.findViewById(R.id.textViewDepartureTime)
        val arrivalTime: TextView = itemView.findViewById(R.id.textViewArrivalTime)
        val transfersValue: TextView = itemView.findViewById(R.id.textViewTransfersValue)
        val transportTagsLayout: LinearLayout = itemView.findViewById(R.id.layoutTransportTags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        val context = holder.itemView.context

        holder.routeName.text = route.routeName
        holder.oldStatusText.visibility = View.GONE

        var timeToFirstVehicleDepartureString = "N/A"
        var firstVehicleStatusMessage = "scheduled"
        // var firstVehicleStatusType = RouteStatusType.ON_TIME // Not strictly needed if dot is gone, color is prime
        var statusTextColor = ContextCompat.getColor(context, R.color.status_on_time)

        val currentTimeMillis = Calendar.getInstance().timeInMillis

        if (route.firstVehicleActualDepartureUTC != null) {
            val diffMillisToDeparture = route.firstVehicleActualDepartureUTC - currentTimeMillis
            val durationMinutesToDeparture = TimeUnit.MILLISECONDS.toMinutes(diffMillisToDeparture)

            timeToFirstVehicleDepartureString = when {
                durationMinutesToDeparture < -1 -> "Departed"
                durationMinutesToDeparture == -1L -> "Departed"
                durationMinutesToDeparture == 0L -> "Due now"
                durationMinutesToDeparture == 1L -> "In 1 min"
                else -> "In $durationMinutesToDeparture min"
            }
        }

        val estimatedDep = route.firstVehicleEstimatedDepartureUTC
        val scheduledDep = route.firstVehicleScheduledDepartureUTC

        if (estimatedDep != null && scheduledDep != null) {
            val diffMillis = scheduledDep - estimatedDep
            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            val thresholdMinutes = 1

            if (estimatedDep == scheduledDep) {
                firstVehicleStatusMessage = "scheduled"
                // firstVehicleStatusType = RouteStatusType.ON_TIME (dot removed)
                statusTextColor = ContextCompat.getColor(context, R.color.status_on_time)
            } else if (diffMinutes > thresholdMinutes) {
                firstVehicleStatusMessage = "${diffMinutes}m early"
                // firstVehicleStatusType = RouteStatusType.EARLY (dot removed)
                statusTextColor = ContextCompat.getColor(context, R.color.status_early)
            } else if (diffMinutes < -thresholdMinutes) {
                firstVehicleStatusMessage = "${-diffMinutes}m delayed"
                // firstVehicleStatusType = RouteStatusType.DELAYED (dot removed)
                statusTextColor = ContextCompat.getColor(context, R.color.status_delayed)
            } else {
                firstVehicleStatusMessage = "on time"
                // firstVehicleStatusType = RouteStatusType.ON_TIME (dot removed)
                statusTextColor = ContextCompat.getColor(context, R.color.status_on_time)
            }
        } else if (route.firstVehicleActualDepartureUTC != null) {
            firstVehicleStatusMessage = "scheduled"
            // firstVehicleStatusType = RouteStatusType.ON_TIME (dot removed)
            statusTextColor = ContextCompat.getColor(context, R.color.status_on_time)
        }

        holder.durationAndStatusText.text =
            "$timeToFirstVehicleDepartureString - $firstVehicleStatusMessage".trimEnd { it == ' ' || it == '-' }
        holder.durationAndStatusText.setTextColor(statusTextColor)

        // Removed statusDot image resource setting as the ImageView is removed
        // when (firstVehicleStatusType) { ... }

        holder.departureTime.text = route.overallJourneyDepartureTimeForDisplay
        holder.arrivalTime.text = route.overallJourneyETAForDisplay
        holder.transfersValue.text = route.transfersCount.toString()

        holder.transportTagsLayout.removeAllViews()
        for ((index, tagData) in route.transportTags.withIndex()) {
            val tagView = LayoutInflater.from(context)
                .inflate(R.layout.partial_transport_tag, holder.transportTagsLayout, false)
            val icon: ImageView = tagView.findViewById(R.id.tag_icon)
            val text: TextView = tagView.findViewById(R.id.tag_text)

            icon.setImageResource(tagData.iconResId)
            text.text = tagData.text

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index < route.transportTags.size - 1) {
                params.marginEnd = context.resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            tagView.layoutParams = params
            holder.transportTagsLayout.addView(tagView)
        }
    }

    override fun getItemCount() = routes.size

    fun updateRoutes(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
}