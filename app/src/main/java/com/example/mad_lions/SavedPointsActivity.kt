package com.example.mad_lions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay

class SavedPointsActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var db: FirebaseFirestore
    private var locationListener: ListenerRegistration? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocationGeo: GeoPoint? = null
    private var userMarker: Marker? = null
    private var followUser = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val CONNECTION_THRESHOLD_METERS = 100.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_points)

        // Configuración OSM
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Brújula
        CompassOverlay(this, map).apply {
            enableCompass()
            map.overlays.add(this)
        }

        // Barra de escala
        val dm = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
        ScaleBarOverlay(map).apply {
            setCentred(true)
            setScaleBarOffset(dm.widthPixels / 2, 50)
            map.overlays.add(this)
        }

        // Long press -> coordenadas
        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let {
                    Toast.makeText(
                        this@SavedPointsActivity,
                        "Coordenadas: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(eventsReceiver))

        // Zoom In
        findViewById<FloatingActionButton>(R.id.fabZoomIn).setOnClickListener {
            map.controller.zoomIn()
        }
        // Zoom Out
        findViewById<FloatingActionButton>(R.id.fabZoomOut).setOnClickListener {
            map.controller.zoomOut()
        }

        // Seguir usuario toggle
        findViewById<FloatingActionButton>(R.id.fabFollow).apply {
            alpha = 0.5f
            setOnClickListener {
                followUser = !followUser
                alpha = if (followUser) 1f else 0.5f
            }
        }

        // Style switch as FAB
        findViewById<FloatingActionButton>(R.id.btnStyleSwitch).setOnClickListener {
            val styles = listOf(
                "OSM" to TileSourceFactory.MAPNIK,
                "Satélite" to TileSourceFactory.USGS_SAT,
                "Topográfico" to TileSourceFactory.USGS_TOPO
            )
            val names = styles.map { it.first }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Estilo de mapa")
                .setItems(names) { _, idx ->
                    map.setTileSource(styles[idx].second)
                }
                .show()
        }

        // Back button
        findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            finish()
        }

        // Obtener ubicación y dibujar
        getCurrentLocation()
        loadLocations()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val geo = GeoPoint(it.latitude, it.longitude)
                currentLocationGeo = geo
                map.controller.setCenter(geo)
                map.controller.setZoom(15.0)

                // Círculo de precisión con Polygon
                val precisionCircle = Polygon(map).apply {
                    fillColor = Color.argb(50, 30, 144, 255)
                    strokeColor = Color.BLUE
                    strokeWidth = 2f
                    points = generateCirclePoints(geo, it.accuracy.toDouble())
                }
                map.overlays.add(precisionCircle)

                // Marcador usuario
                userMarker?.let { map.overlays.remove(it) }
                userMarker = Marker(map).apply {
                    position = geo
                    icon = ContextCompat.getDrawable(
                        this@SavedPointsActivity,
                        R.drawable.ic_user_location
                    )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Mi ubicación"
                }
                map.overlays.add(userMarker)

                if (followUser) map.controller.setCenter(geo)
                map.invalidate()
            } ?: Log.e("Location", "No se pudo obtener la ubicación actual")
        }
    }

    private fun loadLocations() {
        locationListener = db.collection("locations")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "Error obteniendo ubicaciones", e)
                    return@addSnapshotListener
                }

                map.overlays.clear()
                val puntos = mutableListOf<GeoPoint>()

                snapshots?.forEach { doc ->
                    val name = doc.getString("name") ?: "Sin nombre"
                    val description = doc.getString("description") ?: "Sin descripción"
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lon = doc.getDouble("longitude") ?: 0.0
                    val geoPoint = GeoPoint(lat, lon)

                    val marker = Marker(map).apply {
                        position = geoPoint
                        icon = ContextCompat.getDrawable(
                            this@SavedPointsActivity,
                            R.drawable.ic_fountain
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = name
                        snippet = currentLocationGeo?.distanceToAsDouble(geoPoint)?.let { d ->
                            "$description\nDistancia: ${d.toInt()} m"
                        } ?: description
                    }
                    map.overlays.add(marker)
                    puntos.add(geoPoint)
                }

                // Unir puntos
                for (i in puntos.indices) {
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

    private fun generateCirclePoints(
        center: GeoPoint,
        radiusMeters: Double,
        segments: Int = 64
    ): List<GeoPoint> {
        val results = mutableListOf<GeoPoint>()
        val lat = Math.toRadians(center.latitude)
        val lon = Math.toRadians(center.longitude)
        val d = radiusMeters / 6371000.0
        for (i in 0..segments) {
            val theta = 2.0 * Math.PI * i / segments
            val lat2 = Math.asin(
                Math.sin(lat) * Math.cos(d) +
                        Math.cos(lat) * Math.sin(d) * Math.cos(theta)
            )
            val lon2 = lon + Math.atan2(
                Math.sin(theta) * Math.sin(d) * Math.cos(lat),
                Math.cos(d) - Math.sin(lat) * Math.sin(lat2)
            )
            results.add(GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2)))
        }
        return results
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove()
    }
}
