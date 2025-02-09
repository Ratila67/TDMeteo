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

class MainActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var citiesAdapter: CitiesAdapter
    private lateinit var recyclerView: RecyclerView

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
        setContentView(R.layout.activity_main)

        val fab: FloatingActionButton = findViewById(R.id.buttonAddVille)
        fab.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            searchLauncher.launch(intent) // utiliser le launcher au lieu de startActicityForResult
        }
        //initiailiser le recycler view et l'adaptateur
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        citiesAdapter = CitiesAdapter().also { adapter ->
            recyclerView.adapter = adapter
        }

        //ecoute les changements dans firebase
        listenToFirebaseChanges()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun listenToFirebaseChanges() {
        database.child("cities").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(
                    "MainActivity",
                    "Firebase data changed, snapshot has ${snapshot.childrenCount} items"
                )
                val cities = mutableListOf<CityWeather>()
                for (citySnapshot in snapshot.children) {
                    citySnapshot.getValue(CityWeather::class.java)?.let {
                        Log.d("MainActivity", "Adding city: ${it.cityName}")
                        cities.add(it)
                    }
                }
                Log.d("MainActivity", "Submitting ${cities.size} cities to adapter")

                //utiliser une nouvelle liste
                citiesAdapter.submitList(ArrayList(cities))
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Erreur lors de la récupération des données",
                    Toast.LENGTH_SHORT
                ).show()
            }

        })
    }
}