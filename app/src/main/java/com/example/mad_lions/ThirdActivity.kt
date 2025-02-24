package com.example.mad_lions

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ThirdActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.third_activity) // Inflamos el XML

        // Botón para regresar a MainActivity
        val button = findViewById<Button>(R.id.button_return_third)
        button.setOnClickListener {
            finish() // Cierra esta actividad y regresa a la anterior
        }
    }
}
