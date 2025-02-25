package com.example.mad_lions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class SecondActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity)

        locationTextView = findViewById(R.id.text_location)
        webView = findViewById(R.id.mapView)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar WebView para OpenStreetMap
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // Habilitar JS para Leaflet

        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/map.html") // Cargar el mapa localmente

        val buttonLocation = findViewById<Button>(R.id.button_get_location)
        buttonLocation.setOnClickListener {
            getLastLocation()
        }

        val buttonReturn = findViewById<Button>(R.id.button_return)
        buttonReturn.setOnClickListener {
            finish()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    locationTextView.text = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                    webView.evaluateJavascript("updateMap(${location.latitude}, ${location.longitude});", null)
                } else {
                    Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        }
    }
}
