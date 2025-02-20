package com.example.tdmeteo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tdmeteo.adapter.CitiesAdapter
import com.example.tdmeteo.data.CityWeather
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class ListActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var citiesAdapter: CitiesAdapter
    private lateinit var recyclerView: RecyclerView
    private val TAG = "ListActivity"
    private var valueEventListener: ValueEventListener? = null

    //def le launcher pour activite de recherche
    private val searchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            listenToFirebaseChanges()

            result.data?.getStringExtra("city_name")?.let { cityName ->
                Toast.makeText(this, "Ville ajoutée: $cityName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list)

        setupRecyclerView()

        val fab: FloatingActionButton = findViewById(R.id.buttonAddVille)
        fab.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            searchLauncher.launch(intent) // utiliser le launcher au lieu de startActivityForResult
        }

        //ecoute les changements dans firebase
        listenToFirebaseChanges()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        citiesAdapter = CitiesAdapter().apply {
            // Ajouter le gestionnaire de clic
            setOnItemClickListener { city ->
                Log.d(TAG, "Clicked on city: ${city.cityName}, temp : ${city.temperature}")

                //Créer un nouvel intent avec flag activity reorder to front
                val intent = Intent(this@ListActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                intent.putExtra("city_name", city.cityName)
                intent.putExtra("city_country", city.country)
                intent.putExtra("city_temperature", city.temperature)
                intent.putExtra("city_condition", city.weatherCondition)

                //Verifier que les extras sont correctement définis
                Log.d(TAG, "Intent extras before start: ${intent.extras?.keySet()?.joinToString()}")
                Log.d(TAG,"City name in intent: ${intent.getStringExtra("city_name")}")

                //Démarrer mainActivity sans la recreer
                startActivity(intent)
                finish()
            }
        }
        recyclerView.adapter = citiesAdapter ?: return
    }

    private fun listenToFirebaseChanges() {
            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                Log.d(TAG, "Firebase data changed, snapshot has ${snapshot.childrenCount} items")

                val cities = mutableListOf<CityWeather>()
                for (citySnapshot in snapshot.children) {
                    citySnapshot.getValue(CityWeather::class.java)?.let {
                        Log.d(TAG, "Adding city: ${it.cityName}")
                        cities.add(it)
                    }
                }
                Log.d(TAG, "Submitting ${cities.size} cities to adapter")

                //utiliser une nouvelle liste
                citiesAdapter.submitList(ArrayList(cities))
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ListActivity,
                    "Erreur lors de la récupération des données",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
            database.child("cities").addValueEventListener(valueEventListener!!)
        }

    override fun onDestroy() {
        super.onDestroy()
        //supprimer le listener lorsque l'activité est détruite
        valueEventListener?.let {
            database.child("cities").removeEventListener(it)
    }
    }
}