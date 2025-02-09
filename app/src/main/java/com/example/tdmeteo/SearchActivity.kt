package com.example.tdmeteo


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlin.math.abs
import androidx.core.widget.addTextChangedListener
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Response
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.example.tdmeteo.data.CityWeather
import android.util.Log
import com.example.tdmeteo.api.CityResponse
import com.example.tdmeteo.api.WeatherResponse
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.tdmeteo.api.TomorrowApi
import com.example.tdmeteo.api.GeoDbApi
import com.example.tdmeteo.api.GeoDbResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class SearchActivity : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var resultsListView: ListView
    private val API_KEY = TomorrowApi.API_KEY // Remplacer par la clé API Tommorow
    private var selectedCities = mutableListOf<CityResponse>()
    private val database = FirebaseDatabase.getInstance().reference
    private var searchJob: Job? = null


    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.tomorrow.io/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherApi = retrofit.create(TomorrowApi::class.java)

    private val geoDbRetrofit = Retrofit.Builder()
        .baseUrl("https://wft-geo-db.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val geoDbApi = geoDbRetrofit.create(GeoDbApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        //initialisation des vues
        searchEditText = findViewById(R.id.searchEditText)
        resultsListView = findViewById(R.id.resultsListView)

        //configuration de la listview avec un adapter vide initial
        resultsListView.adapter = ArrayAdapter(this, R.layout.item_city, mutableListOf<String>())

        searchEditText.addTextChangedListener { text ->
            if (text?.length ?: 0 >= 3) {
                searchCities(text.toString())
            }
        }

        resultsListView.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(
                this,
                "Ville sélectionnée: ${selectedCities[position].name}",
                Toast.LENGTH_SHORT
            ).show()
            val selectedCity = selectedCities[position]
            fetchWeatherAndSave(selectedCity)
        }
    }

    private fun searchCities(query: String) {
        searchJob?.cancel()

        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            if (query.length >= 3) {
                geoDbApi.searchCity(query).enqueue(object : retrofit2.Callback<GeoDbResponse> {
                    override fun onResponse(
                        call: Call<GeoDbResponse>,
                        response: Response<GeoDbResponse>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let { geoDbResponse ->
                                selectedCities = geoDbResponse.data.map { city ->
                                    CityResponse(
                                        name = city.city,
                                        lat = city.latitude,
                                        lon = city.longitude,
                                        country = city.countryCode
                                    )
                                }.toMutableList()

                                val cityNames = selectedCities.map { "${it.name}, ${it.country}" }
                                runOnUiThread {
                                    val adapter = ArrayAdapter(
                                        this@SearchActivity,
                                        R.layout.item_city,
                                        cityNames
                                    )
                                    resultsListView.adapter = adapter
                                }
                            }
                        } else {
                            Log.e("SearchActivity", "Erreur : ${response.errorBody()?.string()}")
                            runOnUiThread {
                                Toast.makeText(
                                    this@SearchActivity,
                                    "Erreur lors de la recherche des villes",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }


                    override fun onFailure(call: Call<GeoDbResponse>, t: Throwable) {
                        Log.e("SearchActivity", "Erreur lors de la recherche des villes", t)
                        runOnUiThread {
                            Toast.makeText(
                                this@SearchActivity,
                                "Erreur de connexion",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            }
        }
    }



    private fun fetchWeatherAndSave(city: CityResponse) {
        Log.d("SearchActivity", "fetchWeatherAndSave called for city: ${city.name}")
        val location = "${city.lat},${city.lon}"
        val weatherCall = weatherApi.getWeather(
            location = location,
            apiKey = API_KEY,
        )

        weatherCall.enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { weatherResponse ->
                        Log.d(
                            "SearchActivity", """
                            Weather data:
                            - Temperature : ${weatherResponse.data.values.temperature}
                            - Wdeather Code : ${weatherResponse.data.values.weatherCode}
                            - Time : ${weatherResponse.data.time}
                        """.trimIndent()
                        )

                        val cityWeather = CityWeather(
                            cityName = city.name,
                            country = city.country,
                            temperature = weatherResponse.data.values.temperature,
                            weatherCondition = getWeatherCondition(
                                weatherResponse.data.values.weatherCode,
                                weatherResponse.data.time,
                                weatherResponse.data.values.sunriseTime,
                                weatherResponse.data.values.sunsetTime
                            ),
                            lat = city . lat,
                            lon = city.lon
                        )
                        saveToFirebase(cityWeather)
                    }
                } else {
                    Log.e("SearchActivity", "Erreur : ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@SearchActivity,
                        "Erreur de récupération météo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("SearchActivity", "Échec de l'appel API", t)
                Toast.makeText(this@SearchActivity, "Échec de l'appel météo", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }


    private fun getWeatherCondition(
        weatherCode: Int,
        currentTime: String,
        sunrise: String?,
        sunset: String?
    ): String {
        //ajout de log
        Log.d(
            "SearchActivity",
            "Weather check - Weather ID: $weatherCode, Current Time: $currentTime, Sunrise: $sunrise, Sunset: $sunset"
        )

        // convertir les timestamps en date
        // si sunrise ou sunset est null, on se base uniquement sur le code meteo
        if (sunrise == null || sunset == null) {
            return when (weatherCode) {
                1000 -> "CLEAR" // ciel degagé
                1100, 1101 -> "PARTLY_CLOUDY" //Partiellement nuageux
                1102, 1103 -> "CLOUDS" // Nuageux
                4000, 4001, 4200, 4201 -> "RAIN" // Pluie
                5000, 5001, 5100, 5101 -> "SNOW" // Neige
                8000 -> "STORM" // Orage
                else -> "CLEAR"
            }
        }
        val current = currentTime.toDate()
        val sunriseTime = sunrise.toDate()
        val sunsetTime = sunset.toDate()

        val isNight = current.before(sunriseTime) || current.after(sunsetTime)

        return if (isNight) {
            "NIGHT"
        } else {
            when (weatherCode) {
                1000 -> "CLEAR" // ciel degagé
                1100, 1101 -> "PARTLY_CLOUDY" //Partiellement nuageux
                1102, 1103 -> "CLOUDS" // Nuageux
                4000, 4001, 4200, 4201 -> "RAIN" // Pluie
                5000, 5001, 5100, 5101 -> "SNOW" // Neige
                8000 -> "STORM" // Orage
                else -> "CLEAR"
            }
        }
    }

    private fun String.toDate(): Date {
        return try {
            //Gerer le format ISO8601 avec le Z
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(this) ?: Date()
        } catch (e: Exception) {
            Log.e("SearchActivity", "Erreur de parsing de date: $this", e)
            Date()
        }
    }

    private fun saveToFirebase(cityWeather: CityWeather) {
        //ajout d'un log
        Log.d("SearchActivity", "Saving to Firebase: ${cityWeather.cityName}")
        //on enregistre dans firebase
        val cityRef = database.child("cities").push()
        cityRef.setValue(cityWeather).addOnSuccessListener {
            // on créé un intent avec le resultat
            Log.d("SearchActivity", "Success saved to Firebase")

            val resultIntent = Intent().apply {
                putExtra("city_name", cityWeather.cityName)
                putExtra("country", cityWeather.country)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
            .addOnFailureListener {
                Log.e("SearchActivity", "Failed to save to Firebase", it)
                Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() //revenir arriere fleche toolbar
        return true
    }
}