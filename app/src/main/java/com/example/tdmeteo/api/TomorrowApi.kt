package com.example.tdmeteo.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface TomorrowApi {
    companion object {
        const val API_KEY = ApiKeys.TOMORROW_API_KEY
    }

    @GET("v4/weather/realtime")
    fun getWeather(
        @Query("location") location: String, // ex: "48.8566,2.3522" pour Paris
        @Query("apikey") apiKey: String = API_KEY, // clé API
        @Query("units") units: String = "metric" // mesure
    ): Call<WeatherResponse>
}

data class WeatherResponse(
    val data: WeatherData,
    val location: Location,
)

data class WeatherData(
    val values: WeatherValues,
    val time: String
)

data class WeatherValues(
    val temperature: Double,  // Température en °C
    val weatherCode: Int,      // Code météo selon Tomorrow.io
    val sunriseTime: String,
    val sunsetTime: String,
)


data class Location(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String
)

data class CityResponse(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double
) {
    fun toMutableList() {
        TODO("Not yet implemented")
    }
}