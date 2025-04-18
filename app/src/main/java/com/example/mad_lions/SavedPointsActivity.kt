package com.example.mad_lions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat

class SavedPointsActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var db: FirebaseFirestore
    private var locationListener: ListenerRegistration? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_points)

        // Configuración de OSM
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener la ubicación actual del usuario y centrar el mapa
        getCurrentLocation()

        // Cargar las ubicaciones guardadas
        loadLocations()

        findViewById<FloatingActionButton>(R.id.fabBack)?.setOnClickListener {
            finish()
        }

    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = GeoPoint(location.latitude, location.longitude)
                map.controller.setCenter(userLocation)
                map.controller.setZoom(15.0)  // Ajustar el zoom para que se vea bien
            } else {
                Log.e("Location", "No se pudo obtener la ubicación actual")
            }
        }
    }

    private fun loadLocations() {
        locationListener = db.collection("locations")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "Error obteniendo ubicaciones", e)
                    return@addSnapshotListener
                }

                // Limpiar el mapa
                map.overlays.clear()

                snapshots?.forEach { document ->
                    val name        = document.getString("name") ?: "Sin nombre"
                    val description = document.getString("description") ?: "Sin descripción"
                    val lat         = document.getDouble("latitude")  ?: 0.0
                    val lon         = document.getDouble("longitude") ?: 0.0

                    val marker = Marker(map).apply {
                        position = GeoPoint(lat, lon)
                        icon = ContextCompat.getDrawable(this@SavedPointsActivity, R.drawable.ic_fountain)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title   = name
                        snippet = description
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove() // Detener la escucha de Firestore cuando la actividad se destruye
    }
}
