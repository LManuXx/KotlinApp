package com.example.mad_lions

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Inflamos el XML

        // Botón para ir a SecondActivity
        val buttonSecond = findViewById<Button>(R.id.button_second)
        buttonSecond.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        // Botón para ir a ThirdActivity
        val buttonThird = findViewById<Button>(R.id.button_third)
        buttonThird.setOnClickListener {
            startActivity(Intent(this, ThirdActivity::class.java))
        }
    }
}
