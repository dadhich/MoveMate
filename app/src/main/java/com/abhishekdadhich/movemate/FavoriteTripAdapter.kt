package com.abhishekdadhich.movemate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteTripAdapter(
    private var favorites: List<FavoriteTrip>,
    private val onItemClick: (FavoriteTrip) -> Unit,
    private val onDeleteClick: (FavoriteTrip) -> Unit
) : RecyclerView.Adapter<FavoriteTripAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewFavoriteName)
        val textViewRoute: TextView = itemView.findViewById(R.id.textViewFavoriteRoute)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorite = favorites[position]
        holder.textViewName.text = favorite.name
        holder.textViewRoute.text = "${favorite.originName} to ${favorite.destinationName}"
        holder.itemView.setOnClickListener { onItemClick(favorite) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(favorite) }
    }

    override fun getItemCount(): Int = favorites.size

    // New function to update favorites list
    fun updateFavorites(newFavorites: List<FavoriteTrip>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }
}