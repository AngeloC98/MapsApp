package com.example.mapsapp.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapsapp.R
import com.example.mapsapp.model.Pin
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private var db = FirebaseFirestore.getInstance()

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var user: FirebaseUser

    private val pins = mutableListOf<Pin>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val FIREBASE_REQUEST_CODE = 2
    private val INTENT_COMPLETE_CODE= 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initViews()
    }

    private fun initViews() {
        //  Available register options
        providers = listOf<AuthUI.IdpConfig>(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        // Init location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        showSignInOptions()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fab.setOnClickListener {
            val intent = Intent(this, AddPin::class.java)
            startActivityForResult(intent, INTENT_COMPLETE_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPins() {
        db.collection("pins")
            .whereEqualTo("user", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val pinData = Pin(
                        id = document.id,
                        date = document.data["date"] as String?,
                        description = document.data["description"] as String?,
                        imageUrl = document.data["imageUrl"] as String?,
                        latitude = document.data["latitude"] as Double?,
                        longitude = document.data["longitude"] as Double?,
                        user = document.data["user"] as String?
                    )
                    pins.add(pinData)
                    for (pin in pins) {
                        val mapData = LatLng(pin.latitude!!, pin.longitude!!)
                        mMap.addMarker(MarkerOptions().position(mapData).title(pin.id))
                        mMap.setOnMarkerClickListener { marker ->
                            val intent = Intent(this, PinDetail::class.java)
                            intent.putExtra("pin", marker.title)
                            startActivityForResult(intent, INTENT_COMPLETE_CODE)
                            true
                        }
                    }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check if location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Set camera to last location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                val latitude = location?.latitude
                val longitude = location?.longitude
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            latitude!!,
                            longitude!!
                        ), 16F
                    )
                )
            }
    }

    private fun showSignInOptions() {
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.AppTheme)
                .build(), FIREBASE_REQUEST_CODE
        )
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FIREBASE_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                user = FirebaseAuth.getInstance().currentUser!!
                getPins()
            } else {
                Toast.makeText(this, "" + response!!.error!!, Toast.LENGTH_SHORT).show()
            }
        }
        if(requestCode == INTENT_COMPLETE_CODE) {
            mMap.clear()
            pins.clear()
            getPins()
        }
    }
}
