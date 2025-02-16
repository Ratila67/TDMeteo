package com.example.tdmeteo
import androidx.activity.result.contract.ActivityResultContracts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tdmeteo.api.TomorrowApi
import com.example.weatherapp.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AccueilActivity : ComponentActivity() {
    private var startX = 0f
    private var endX = 0f

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val API_KEY = TomorrowApi.API_KEY // Remplacer par la clé API Tommorow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil) // Utilisation du XML au lieu de Jetpack Compose

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Récupération des TextView du layout
        val tvLocation = findViewById<TextView>(R.id.tv_location)
        val tvTemperature = findViewById<TextView>(R.id.tv_temperature)


        checkLocationPermission { latitude, longitude ->
            fetchWeather(latitude, longitude) { temp, city, description ->
                runOnUiThread {
                    tvLocation.text = city
                    tvTemperature.text = temp
                    findViewById<TextView>(R.id.tv_weatherDescription).text = description
                }
            }
        }

    }

    private fun checkLocationPermission(onLocationAvailable: (Double, Double) -> Unit) {
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted) {
            getCurrentLocation(onLocationAvailable)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation { latitude, longitude ->
                fetchWeather(latitude, longitude) { _, _,_ -> }
            }
        } else {
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation(onLocationAvailable: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission non accordée", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationAvailable(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Impossible d'obtenir la localisation", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchWeather(
        latitude: Double,
        longitude: Double,
        onWeatherFetched: (String, String, String) -> Unit
    ) {
        val location = "$latitude,$longitude"
        val url = "https://api.tomorrow.io/v4/timelines?location=$location&fields=temperature,weatherCode&timesteps=1h&units=metric&apikey=$API_KEY"


        Log.d("WeatherApp", "URL API: $url") // 🔍 Voir l'URL appelée

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(this@AccueilActivity, Locale.getDefault())
                val addressList = geocoder.getFromLocation(latitude, longitude, 1)
                val city = addressList?.firstOrNull()?.locality ?: "Ville inconnue"

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
                    onWeatherFetched("${hourlyForecast?.first()?.second ?: "0"}°", city, getWeatherDescription(response.data.timelines.firstOrNull()?.intervals?.firstOrNull()?.values?.weatherCode ?: 0))
                    updateHourlyForecast(hourlyForecast)
                    updateDailyForecast(dailyForecast)
                }
            } catch (e: Exception) {
                Log.e("WeatherApp", "Erreur API : ${e.message}")
                runOnUiThread {
                    onWeatherFetched("Erreur", "Inconnue", "Erreur météo")
                }
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
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Commence par le jour suivant

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

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val result = super.dispatchTouchEvent(event)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // Sauvegarder la position de départ du touché
                startX = event.x
                Log.d("SwipeDetection", "ACTION_DOWN: startX = $startX")
            }
            MotionEvent.ACTION_MOVE -> {
                // Affichage du mouvement en temps réel
                Log.d("SwipeDetection", "ACTION_MOVE: currentX = ${event.x}")
            }
            MotionEvent.ACTION_UP -> {
                // Sauvegarder la position finale du touché
                endX = event.x
                Log.d("SwipeDetection", "ACTION_UP: endX = $endX")

                // Calculer la distance du swipe
                val distance = endX - startX
                Log.d("SwipeDetection", "Swipe distance = $distance")

                // Si la distance est suffisamment grande, on considère qu'il s'agit d'un swipe
                if (distance > 20) {  // Swipe à droite, seuil réduit à 20px
                    Log.d("SwipeDetection", "Swipe à droite détecté")
                    startActivity(Intent(this, MainActivity::class.java))
                    return true
                } else if (distance < -20) {  // Swipe à gauche, seuil réduit à 20px
                    Log.d("SwipeDetection", "Swipe à gauche détecté")
                    startActivity(Intent(this, MainActivity::class.java))
                    return true
                }
            }
        }

        return result
    }


    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Inconnu"
            1000 -> "Ciel clair, ensoleillé"
            1100 -> "Principalement clair"
            1101 -> "Partiellement nuageux"
            1102 -> "Principalement nuageux"
            1001 -> "Nuageux"
            2000 -> "Brouillard"
            2100 -> "Brouillard léger"
            4000 -> "Bruine"
            4001 -> "Pluie"
            4200 -> "Pluie légère"
            4201 -> "Pluie forte"
            5000 -> "Neige"
            5001 -> "Neige légère"
            5100 -> "Flocons de neige"
            5101 -> "Neige abondante"
            6000 -> "Bruine glacée"
            6001 -> "Pluie glacée"
            6200 -> "Pluie glacée légère"
            6201 -> "Pluie glacée forte"
            7000 -> "Grésil"
            7101 -> "Grésil fort"
            7102 -> "Grésil léger"
            8000 -> "Orage"
            else -> "Inconnu ($weatherCode)"
        }
    }
}
class HourlyForecastAdapter(private val hourlyForecast: List<Pair<String, Double>>) : RecyclerView.Adapter<HourlyForecastAdapter.HourlyForecastViewHolder>() {

    class HourlyForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewTime: TextView = view.findViewById(R.id.textViewHourlyTime)
        val textViewTemp: TextView = view.findViewById(R.id.textViewHourlyForecast)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hourly_forecast, parent, false)
        return HourlyForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyForecastViewHolder, position: Int) {
        val (time, temperature) = hourlyForecast[position]
        holder.textViewTime.text = time
        holder.textViewTemp.text = "${temperature}°C"
    }

    override fun getItemCount(): Int = hourlyForecast.size
}

class DailyForecastAdapter(private val dailyForecast: List<Pair<String, Double>>) : RecyclerView.Adapter<DailyForecastAdapter.DailyForecastViewHolder>() {

    class DailyForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewDay: TextView = view.findViewById(R.id.textViewDailyDay)
        val textViewTemp: TextView = view.findViewById(R.id.textViewDailyForecast)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_forecast, parent, false)
        return DailyForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int) {
        val (day, temperature) = dailyForecast[position]
        holder.textViewDay.text = day
        holder.textViewTemp.text = "${temperature}°C"
    }

    override fun getItemCount(): Int = dailyForecast.size
}

