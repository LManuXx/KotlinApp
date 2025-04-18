package com.example.mad_lions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.drawerlayout.widget.DrawerLayout

class AddLocationActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    // ---------- UI ----------
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSaveLocation: Button

    // ---------- Firebase & Location ----------
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  onCreate
    // ──────────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)

        // Drawer Layout & NavigationView
        drawer = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        toggle = ActionBarDrawerToggle(
            this, drawer,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Añadir Fuente"

        // --------- UI refs ----------
        etName = findViewById(R.id.etName)
        etDescription = findViewById(R.id.etDescription)
        btnSaveLocation = findViewById(R.id.btnSaveLocation)
        findViewById<FloatingActionButton>(R.id.fabBack)?.setOnClickListener { finish() }

        // --------- Firebase / location ----------
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnSaveLocation.setOnClickListener {
            if (currentLocation == null) {
                Toast.makeText(this, "Esperando ubicación, intenta de nuevo", Toast.LENGTH_SHORT).show()
            } else saveLocation()
        }

        requestLocationUpdates()
    }

    // ─────────────────────────────────────────── drawer / toolbar callbacks
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (toggle.onOptionsItemSelected(item)) true
        else super.onOptionsItemSelected(item)

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home    -> startActivity(Intent(this, InitialActivity::class.java))
            R.id.nav_map     -> startActivity(Intent(this, SavedPointsActivity::class.java))
            R.id.nav_add     -> { /* ya estamos aquí */ }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    // ─────────────────────────────────────────── location
    private fun requestLocationUpdates() {
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
                currentLocation = location
            } else {
                val locationRequest = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = 5000
                    fastestInterval = 2000
                }
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        currentLocation = result.lastLocation
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        } else {
            Toast.makeText(this, "Se necesitan permisos de ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────── Firestore
    private fun saveLocation() {
        val name = etName.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (name.isEmpty() || description.isEmpty() || currentLocation == null) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "name" to name,
            "description" to description,
            "latitude" to currentLocation!!.latitude,
            "longitude" to currentLocation!!.longitude,
            "userId" to auth.currentUser?.uid
        )

        db.collection("locations")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error guardando ubicación: ${e.message}", e)
                Toast.makeText(this, "Error guardando ubicación: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
