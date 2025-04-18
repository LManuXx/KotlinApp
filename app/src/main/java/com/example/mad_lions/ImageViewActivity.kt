package com.example.mad_lions

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView

class ImageViewActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        // ─── Drawer & NavigationView ─────────────────────────────────────────────
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
        supportActionBar?.title = "Imagen"

        // ─── Carga de la imagen (Glide) ─────────────────────────────────────────
        val imageView = findViewById<ImageView>(R.id.imageView)
        intent.getStringExtra("imageUrl")?.let { url ->
            Glide.with(this).load(url).into(imageView)
        }

        // FAB atrás
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fabBack
        )?.setOnClickListener { finish() }
    }

    // ─── Drawer callbacks ──────────────────────────────────────────────────────
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
            R.id.nav_add     -> startActivity(Intent(this, AddLocationActivity::class.java))
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}
