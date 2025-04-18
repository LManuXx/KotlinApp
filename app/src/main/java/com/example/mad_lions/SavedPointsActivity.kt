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
import android.graphics.Color
import org.osmdroid.views.overlay.Polyline


class SavedPointsActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var db: FirebaseFirestore
    private var locationListener: ListenerRegistration? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val CONNECTION_THRESHOLD_METERS = 1000000.0


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

                // 1) Limpia todo
                map.overlays.clear()

                // 2) Acumula puntos y crea marcadores
                val puntos = mutableListOf<GeoPoint>()
                snapshots?.forEach { document ->
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lon = document.getDouble("longitude")?: 0.0
                    val geo = GeoPoint(lat, lon)

                    // marcador
                    val marker = Marker(map).apply {
                        position = geo
                        icon     = ContextCompat.getDrawable(
                            this@SavedPointsActivity,
                            R.drawable.ic_fountain
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title   = document.getString("name") ?: "Sin nombre"
                        snippet = document.getString("description") ?: "Sin descripción"
                    }
                    map.overlays.add(marker)

                    puntos.add(geo)
                }

                // 3) Une con líneas azules los puntos cercanos
                for (i in 0 until puntos.size) {
                    for (j in i + 1 until puntos.size) {
                        val a = puntos[i]
                        val b = puntos[j]
                        // calcula distancia en metros
                        val dist = a.distanceToAsDouble(b)
                        if (dist <= CONNECTION_THRESHOLD_METERS) {
                            val poly = Polyline(map).apply {
                                setPoints(listOf(a, b))
                                color   = Color.BLUE
                                width   = 4f
                                isGeodesic = true
                            }
                            map.overlays.add(poly)
                        }
                    }
                }

                // 4) Refresca
                map.invalidate()
            }
    }



    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove() // Detener la escucha de Firestore cuando la actividad se destruye
    }
}
