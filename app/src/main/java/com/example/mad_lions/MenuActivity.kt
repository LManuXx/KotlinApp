package com.example.mad_lions

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        auth = FirebaseAuth.getInstance()

        val btnGoToMap = findViewById<Button>(R.id.btnGoToMap)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnAddLocation = findViewById<Button>(R.id.btnAddLocation)

        // Ir a SecondActivity (Mapa)
        btnGoToMap.setOnClickListener {
            startActivity(Intent(this, SavedPointsActivity::class.java))
        }

        // Añadir ubicacion del agua
        btnAddLocation.setOnClickListener {
            startActivity(Intent(this, AddLocationActivity::class.java))
        }

        // Cerrar sesión y volver al login
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
