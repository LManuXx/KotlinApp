package com.example.mad_lions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay

class SavedPointsActivity : AppCompatActivity() {
    private var userMarker: Marker? = null
    private lateinit var map: MapView
    private lateinit var db: FirebaseFirestore
    private var locationListener: ListenerRegistration? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // guardamos la ubicación actual como GeoPoint
    private var currentLocationGeo: GeoPoint? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        // umbral para unir puntos cercanos (en metros)
        private const val CONNECTION_THRESHOLD_METERS = 100.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_points)

        // Configuración de OSM
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        // Firebase y ubicación
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1) Brújula
        CompassOverlay(this, map).apply {
            enableCompass()
            map.overlays.add(this)
        }

        // 2) Barra de escala
        val dm = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
        ScaleBarOverlay(map).apply {
            setCentred(true)
            setScaleBarOffset(dm.widthPixels / 2, 10) // 10px desde arriba
            map.overlays.add(this)
        }

        // Obtener y centrar en ubicación del usuario
        getCurrentLocation()

        // Cargar ubicaciones y dibujar marcadores, líneas y distancias
        loadLocations()

        // FAB atrás
        findViewById<FloatingActionButton>(R.id.fabBack)?.setOnClickListener {
            finish()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // 1) Centro del mapa
                val geo = GeoPoint(location.latitude, location.longitude)
                currentLocationGeo = geo
                map.controller.setCenter(geo)
                map.controller.setZoom(15.0)

                // 2) Si ya había un marcador, lo quitamos
                userMarker?.let { map.overlays.remove(it) }

                // 3) Creamos uno nuevo con icono distinto
                userMarker = Marker(map).apply {
                    position = geo
                    icon     = ContextCompat.getDrawable(
                        this@SavedPointsActivity,
                        R.drawable.ic_user_location
                    )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title    = "Mi ubicación"
                }
                map.overlays.add(userMarker)
                map.invalidate()
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

                // Limpia marcadores y líneas anteriores
                map.overlays.clear()

                // Acumula puntos para procesar conexiones
                val puntos = mutableListOf<GeoPoint>()

                snapshots?.forEach { doc ->
                    val name        = doc.getString("name") ?: "Sin nombre"
                    val description = doc.getString("description") ?: "Sin descripción"
                    val lat         = doc.getDouble("latitude")  ?: 0.0
                    val lon         = doc.getDouble("longitude") ?: 0.0
                    val geoPoint    = GeoPoint(lat, lon)

                    // Marcador con snippet que incluye distancia si está disponible
                    val marker = Marker(map).apply {
                        position = geoPoint
                        icon     = ContextCompat.getDrawable(
                            this@SavedPointsActivity,
                            R.drawable.ic_fountain
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title   = name
                        snippet = if (currentLocationGeo != null) {
                            val dist = currentLocationGeo!!.distanceToAsDouble(geoPoint).toInt()
                            "$description\nDistancia: ${dist} m"
                        } else {
                            description
                        }
                    }
                    map.overlays.add(marker)
                    puntos.add(geoPoint)
                }

                // Unir puntos cercanos con líneas azules
                for (i in 0 until puntos.size) {
                    for (j in i + 1 until puntos.size) {
                        val a = puntos[i]
                        val b = puntos[j]
                        if (a.distanceToAsDouble(b) <= CONNECTION_THRESHOLD_METERS) {
                            Polyline(map).apply {
                                setPoints(listOf(a, b))
                                color = Color.BLUE
                                width = 4f
                                isGeodesic = true
                            }.also { map.overlays.add(it) }
                        }
                    }
                }

                map.invalidate()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove()
    }
}
