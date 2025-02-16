package com.example.weatherapp.network

import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val data: WeatherData
)

data class WeatherData(
    val timelines: List<Timeline>
)

data class Timeline(
    val intervals: List<Interval>
)

data class Interval(
    val startTime: String,
    val values: Values
)

data class Values(
    val temperature: Double,
    val weatherCode: Int
)

interface WeatherApi {
    @GET("v4/timelines")
    suspend fun getWeather(
        @Query("location") location: String,       // Coordonnées géographiques
        @Query("apikey") apiKey: String,           // Votre clé API
        @Query("fields") fields: String = "temperature,weatherCode",  // Les champs à récupérer
        @Query("timesteps") timesteps: String = "1h",  // Intervalle de temps (1 heure)
        @Query("units") units: String = "metric"    // Unités des températures
    ): WeatherResponse

}
