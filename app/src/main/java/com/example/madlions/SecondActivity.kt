package com.example.madlions

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity) // Inflamos el XML

        // Botón para regresar a MainActivity
        val button = findViewById<Button>(R.id.button_return)
        button.setOnClickListener {
            finish() // Cierra esta actividad y regresa a la anterior
        }
    }
}
