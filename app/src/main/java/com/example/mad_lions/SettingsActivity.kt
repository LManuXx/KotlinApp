package com.example.mad_lions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    // preferencias
    private lateinit var prefs: SharedPreferences

    // vistas
    private lateinit var etUserName: TextInputEditText
    private lateinit var swDarkMode: SwitchMaterial
    private lateinit var swLocation: SwitchMaterial
    private lateinit var btnSave: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var tvEmail: TextView

    // launcher para permiso ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        swLocation.isChecked = granted
        if (!granted) {
            Snackbar.make(swLocation, "Permiso de ubicación denegado", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // flecha back
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Ajustes"

        prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // referencias
        etUserName = findViewById(R.id.etUserName)
        swDarkMode = findViewById(R.id.swDarkMode)
        swLocation = findViewById(R.id.swLocationPermission)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        tvEmail = findViewById(R.id.tvUserEmail)

        // valores actuales
        etUserName.setText(prefs.getString("userName", ""))
        swDarkMode.isChecked = prefs.getBoolean("darkMode", false)
        tvEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: "Invitado"

        swLocation.isChecked = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // listeners
        btnSave.setOnClickListener { savePrefs() }
        btnLogout.setOnClickListener { signOut() }

        swDarkMode.setOnCheckedChangeListener { _, checked ->
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        swLocation.setOnCheckedChangeListener { _, checked ->
            handleLocationSwitch(checked)
        }
    }

    private fun savePrefs() {
        prefs.edit().apply {
            putString("userName", etUserName.text.toString())
            putBoolean("darkMode", swDarkMode.isChecked)
            apply()
        }
        Toast.makeText(this, "Preferencias guardadas", Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun handleLocationSwitch(enable: Boolean) {
        if (enable) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // llevar a ajustes del sistema para revocar
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }.also { startActivity(it) }

            Snackbar.make(
                swLocation,
                "Desactiva el permiso 'Ubicación' y vuelve a la app",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
