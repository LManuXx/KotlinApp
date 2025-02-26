package com.example.mad_lions

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ImageViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val imageView = findViewById<ImageView>(R.id.imageView)

        val imageUrl = intent.getStringExtra("imageUrl")

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(imageView)
        }
    }
}
