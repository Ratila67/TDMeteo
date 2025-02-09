package com.example.tdmeteo.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface GeoDbApi {
    companion object {
        const val API_KEY = ApiKeys.GEODB_API_KEY
    }
    @Headers(
        "X-RapidAPI-Key: $API_KEY",
        "X-RapidApi-Host: wft-geo-db.p.rapidapi.com"
    )
    @GET("v1/geo/cities")
    fun searchCity(
        @Query("namePrefix") query: String,
        @Query("limit") limit: Int = 5,
        @Query("sort") sort: String = "-population",
        @Query("types") types: String = "CITY"
    ): Call<GeoDbResponse>
}

data class GeoDbResponse(
    val data: List<GeoDbCity>
)

data class GeoDbCity(
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String
)
