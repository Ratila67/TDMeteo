package com.example.tdmeteo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tdmeteo.api.TomorrowApi
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.tdmeteo.data.CityWeather
import com.example.weatherapp.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance().reference
    private var API_KEY = TomorrowApi.API_KEY
    private var startX = 0f
    private var endX = 0f
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurer le bouton pour aller à la liste
        findViewById<ImageButton>(R.id.btn_list).setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        // Verifier si on a recu une ville specifique
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val result = super.dispatchTouchEvent(event)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> startX = event.x
            MotionEvent.ACTION_UP -> {
                endX = event.x
                val distance = endX - startX
                if (distance > 20) {
                    startActivity(Intent(this, AccueilActivity::class.java))
                    return true
                } else if (distance < -20) {
                    startActivity(Intent(this, AccueilActivity::class.java))
                    return true
                }
            }
        }
        return result
    }

    private fun handleIntent(intent: Intent) {
        val extras = intent.extras
        Log.d("MainActivity", "Handling intent with extras: ${extras?.keySet()?.joinToString()}")

    if (extras != null) {
        //utiliser les données de l'intent
        val cityName = extras.getString("city_name")
        Log.d("MainActivity", "City name from extras: $cityName")

        if (!cityName.isNullOrEmpty()) {
            val country = extras.getString("city_country", "")
            val temperature = extras.getDouble("city_temperature", 0.0)
            val condition = extras.getString("city_condition", "")

            Log.d(
                "MainActivity",
                "Creating CityWeather with: name=$cityName, country=$country, temp=$temperature, condition=$condition"
            )

            val city = CityWeather(
                cityName = cityName,
                country = country,
                temperature = temperature,
                weatherCondition = condition,
            )
            updateUI(city)
            return
        }
    }
        Log.d("MainActivity", "No valid extras found, loading first city")
    // Sinon charger la premiere ville
        loadFirstCity()
    }

    private fun updateUI(city: CityWeather) {
        Log.d("MainActivity", "Updating UI with city: ${city.cityName}, temp: ${city.temperature}")

        findViewById<TextView>(R.id.tv_location).apply {
            text = "${city.cityName}, ${city.country}"
        }

        findViewById<TextView>(R.id.tv_temperature)?.apply {
            text = String.format(Locale.FRANCE, "%.1f°C", city.temperature)
        }

        // Mettre à jour la description météo
        findViewById<TextView>(R.id.tv_weatherDescription)?.apply {
            text = when (city.weatherCondition) {
            "CLEAR" -> "Ciel dégagé"
            "PARTLY_CLOUDY" -> "Partiellement nuageux"
            "CLOUDS" -> "Nuageux"
            "RAIN" -> "Pluvieux"
            "SNOW" -> "Neigeux"
            "STORM" -> "Orageux"
            "NIGHT" -> "Nuit"
            else -> "Conditions inconnues"
            }
        }
    }

    private fun loadFirstCity() {
        Log.d("MainActivity", "Loading first city from Firebase")
        database.child("cities").limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.firstOrNull()?.getValue(CityWeather::class.java)?.let { city ->
                            updateUI(city)
                            fetchWeather(city.cityName)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Error loading first city", error.toException())
                }
            })
    }
    private fun fetchWeather(cityName: String) {
        val location = "$cityName"
        val url = "https://api.tomorrow.io/v4/timelines?location=$location&fields=temperature,weatherCode&timesteps=1h&units=metric&apikey=$API_KEY"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getWeather(location, API_KEY)

                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val roundedHours = getRoundedHours(currentHour)

                val hourlyForecast = response.data.timelines.firstOrNull()?.intervals?.take(24)?.mapIndexed { index, it ->
                    Pair(roundedHours[index], it.values.temperature)
                }

                val nextDays = getNextDays(calendar.get(Calendar.DAY_OF_WEEK))
                val dailyForecast = response.data.timelines.firstOrNull()?.intervals?.take(5)?.mapIndexed { index, it ->
                    Pair(nextDays[index], it.values.temperature)
                }

                runOnUiThread {
                    updateHourlyForecast(hourlyForecast)
                    updateDailyForecast(dailyForecast)
                }
            } catch (e: Exception) {
                Log.e("WeatherApp", "Erreur API : ${e.message}")
            }
        }
    }
    private fun getRoundedHours(currentHour: Int): List<String> {
        val hours = mutableListOf<String>()
        var startHour = (currentHour + 1) % 24
        for (i in 0 until 24) {
            hours.add(String.format("%02d:00", startHour))
            startHour = (startHour + 1) % 24
        }
        return hours
    }

    private fun getNextDays(currentDay: Int): List<String> {
        val days = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        for (i in 0 until 5) {
            days.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    private fun updateDailyForecast(dailyForecast: List<Pair<String, Double>>?) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDailyForecast)
        dailyForecast?.let {
            recyclerView.adapter = DailyForecastAdapter(it)
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun updateHourlyForecast(hourlyForecast: List<Pair<String, Double>>?) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHourlyForecast)
        hourlyForecast?.let {
            recyclerView.adapter = HourlyForecastAdapter(it)
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        }
    }
}