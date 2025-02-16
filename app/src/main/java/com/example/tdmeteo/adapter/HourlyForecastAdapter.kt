package com.example.tdmeteo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tdmeteo.R

class HourlyForecastAdapter(private val hourlyForecast: List<Pair<String, Double>>) : RecyclerView.Adapter<HourlyForecastAdapter.HourlyForecastViewHolder>() {

    // ViewHolder pour les éléments de la liste des prévisions horaires
    class HourlyForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewTime: TextView = view.findViewById(R.id.textViewHourlyTime)
        val textViewTemp: TextView = view.findViewById(R.id.textViewHourlyForecast)
    }

    // Création des vues pour les éléments de la liste
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hourly_forecast, parent, false)
        return HourlyForecastViewHolder(view)
    }

    // Liaison des données aux vues
    override fun onBindViewHolder(holder: HourlyForecastViewHolder, position: Int) {
        val (time, temperature) = hourlyForecast[position]
        holder.textViewTime.text = time
        holder.textViewTemp.text = "${temperature}°C"
    }

    // Retourne le nombre d'éléments dans la liste
    override fun getItemCount(): Int = hourlyForecast.size
}
