package com.example.mad_lions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.ByteArrayOutputStream
import java.util.*

class AddLocationActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivPhoto: ImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSaveLocation: Button

    private var imageUri: Uri? = null
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)

        etName = findViewById(R.id.etName)
        etDescription = findViewById(R.id.etDescription)
        ivPhoto = findViewById(R.id.ivPhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnSaveLocation = findViewById(R.id.btnSaveLocation)

        storage = FirebaseStorage.getInstance()
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSaveLocation.setOnClickListener {
            saveLocation()
        }

        // Obtener ubicación actual
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            ivPhoto.setImageURI(imageUri)
        }
    }

    private fun saveLocation() {
        val name = etName.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (name.isEmpty() || description.isEmpty() || imageUri == null || currentLocation == null) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.reference.child("images/${UUID.randomUUID()}.jpg")
        val uploadTask = storageRef.putFile(imageUri!!)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val locationData = hashMapOf(
                    "name" to name,
                    "description" to description,
                    "latitude" to currentLocation!!.latitude,
                    "longitude" to currentLocation!!.longitude,
                    "imageUrl" to uri.toString(),
                    "userId" to auth.currentUser?.uid
                )

                db.collection("locations")
                    .add(locationData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error guardando ubicación", e)
                    }
            }
        }
    }
}
