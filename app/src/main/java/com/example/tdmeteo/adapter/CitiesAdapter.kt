package com.example.tdmeteo.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tdmeteo.R
import com.example.tdmeteo.data.CityWeather

class CitiesAdapter : ListAdapter<CityWeather, CitiesAdapter.CityViewHolder>(CityDiffCallback()) {

    init {
        Log.d("CitiesAdapter", "Adapter initialized")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        Log.d("CitiesAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_weather, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        Log.d("CitiesAdapter", "onBindViewHolder called for position $position")
        val item = getItem(position)
        Log.d("CitiesAdapter", "Binding city: ${item.cityName}")
        holder.bind(item)
    }

    override fun submitList(list: List<CityWeather>?) {
        Log.d("CitiesAdapter", "submitList called with ${list?.size} items")
        super.submitList(list)
    }

    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            Log.d("CityViewHolder", "ViewHolder created")
        }

        private val cityName: TextView = itemView.findViewById(R.id.cityName)
        private val temperature: TextView = itemView.findViewById(R.id.temperature)
        private val weatherIcon: ImageView = itemView.findViewById(R.id.weatherIcon)

        fun bind(cityWeather: CityWeather) {
            Log.d("CityViewHolder", "Binding data for city: ${cityWeather.cityName}")
            cityName.text = "${cityWeather.cityName}, ${cityWeather.country}"
            temperature.text = String.format("%.1f°C", cityWeather.temperature)

            val iconRes = when (cityWeather.weatherCondition) {
                "CLEAR" -> R.drawable.ic_sun
                "PARTLY_CLOUDS" -> R.drawable.ic_partly_cloud
                "RAIN" -> R.drawable.ic_rain
                "CLOUDS" -> R.drawable.ic_cloud
                "NIGHT" -> R.drawable.ic_moon
                "SNOW" -> R.drawable.ic_snow
                "STORM" -> R.drawable.ic_storm
                else -> R.drawable.ic_sun
            }
            weatherIcon.setImageResource(iconRes)
        }
    }

    class CityDiffCallback : DiffUtil.ItemCallback<CityWeather>() {
        override fun areItemsTheSame(oldItem: CityWeather, newItem: CityWeather): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: CityWeather, newItem: CityWeather): Boolean {
            return oldItem == newItem
        }
    }
}