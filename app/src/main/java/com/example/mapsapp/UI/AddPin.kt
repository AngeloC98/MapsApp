package com.example.mapsapp.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.R
import com.example.mapsapp.model.Pin
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_add_pin.*
import kotlinx.android.synthetic.main.activity_add_pin.button
import kotlinx.android.synthetic.main.activity_add_pin.ivCamera
import kotlinx.android.synthetic.main.activity_add_pin.tvDate
import kotlinx.android.synthetic.main.activity_add_pin.tvLocation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class AddPin : AppCompatActivity() {
    private var db = FirebaseFirestore.getInstance()
    private var mStorageRef: StorageReference? = null
    var imageUri: Uri? = null
    var downloadUrl: String? = null
    var formattedDate: String? = null
    var latitude: Double? = null
    var longitude: Double? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private val PERMISSION_CODE = 1
    private val IMAGE_CAPTURE_CODE = 2

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pin)

        initViews()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViews() {
        // Init firebase storage
        mStorageRef = FirebaseStorage.getInstance().reference;

        // Get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location ->
                latitude = location.latitude
                longitude = location.longitude
                val address = getAddress(latitude!!, longitude!!)
                tvLocation.text = address
            }

        // Format date
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        formattedDate = current.format(formatter)
        tvDate.text = formattedDate

        if (imageUri != null) ivCamera.setImageURI(imageUri)

        button.setOnClickListener {
            uploadPhoto()
        }

        // Check permissions for opening camera and using storage
        ivCamera.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED
            ) {
                val permission = arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestPermissions(permission, PERMISSION_CODE)
            } else {
                openCamera()
            }
        }

    }

    // Converts LatLong to address
    private fun getAddress(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this)
        val list = geocoder.getFromLocation(lat, lng, 1)
        return list[0].getAddressLine(0)
    }

    // Save to Firestore
    private fun saveToDatabase() {
        val user = FirebaseAuth.getInstance().currentUser
        val pin = Pin(
            user = user!!.uid,
            date = formattedDate,
            latitude = latitude,
            longitude = longitude,
            description = etDescription.text.toString(),
            imageUrl = downloadUrl!!
        )
        val docRef = db.collection("pins").document()
        docRef.set(pin)
        Toast.makeText(this, "Pin created!", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun uploadPhoto() {
        val ref: StorageReference =
            mStorageRef!!.child("images/${UUID.randomUUID()}")

        ref.putFile(imageUri!!).addOnSuccessListener {
            ref.downloadUrl.addOnCompleteListener { task ->
                downloadUrl = task.result.toString()
                saveToDatabase()
            }

        }
    }

    // Opens camera
    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Camera Picture")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    // Checks if permissions was granted or not
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Gets called after picture is taken
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            ivCamera.setImageURI(imageUri)
        }
    }
}
