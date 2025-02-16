package com.example.tdmeteo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tdmeteo.R

class DailyForecastAdapter(private val dailyForecast: List<Pair<String, Double>>) : RecyclerView.Adapter<DailyForecastAdapter.DailyForecastViewHolder>() {

    // ViewHolder pour les éléments de la liste des prévisions journalières
    class DailyForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewDay: TextView = view.findViewById(R.id.textViewDailyDay)
        val textViewTemp: TextView = view.findViewById(R.id.textViewDailyForecast)
    }

    // Création des vues pour les éléments de la liste
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_forecast, parent, false)
        return DailyForecastViewHolder(view)
    }

    // Liaison des données aux vues
    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int) {
        val (day, temperature) = dailyForecast[position]
        holder.textViewDay.text = day
        holder.textViewTemp.text = "${temperature}°C"
    }

    // Retourne le nombre d'éléments dans la liste
    override fun getItemCount(): Int = dailyForecast.size
}
