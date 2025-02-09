package com.example.tdmeteo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.tdmeteo.data.CityWeather
import java.util.*

class MainActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurer le bouton pour aller à la liste
        findViewById<ImageButton>(R.id.btn_list).setOnClickListener {
            startActivity(Intent(this, ListActivity::class.java))
        }

        // Verifier si on a recu une ville specifique
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
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
                            Log.d("MainActivity", "Loaded first city: ${city.cityName}")
                            updateUI(city)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Error loading first city", error.toException())
                }
            })
    }

}