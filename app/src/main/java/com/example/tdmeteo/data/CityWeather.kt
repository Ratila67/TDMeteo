package com.example.tdmeteo.data

data class CityWeather(
    val cityName: String = "",
    val country: String = "",
    val temperature: Double = 0.0,
    val weatherCondition: String = "", //pluie, vent, soleil, etc
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    //constructeur vide pour firebase
    constructor() : this("", "", 0.0, "", 0.0, 0.0)
}