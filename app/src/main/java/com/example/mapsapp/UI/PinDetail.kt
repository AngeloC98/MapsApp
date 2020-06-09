package com.example.mapsapp.ui

import android.location.Geocoder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.R
import com.example.mapsapp.model.Pin
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_pin_detail.*


class PinDetail : AppCompatActivity() {

    private var id: String? = null
    private var pin: Pin? = null
    private var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_detail)

        initViews()
    }

    private fun initViews() {
        //get data from intent
        val intent = intent
        id = intent.getStringExtra("pin")
        setFields()

        button.setOnClickListener { finish() }
        btnDelete.setOnClickListener {
            db.collection("pins").document(id!!)
                .delete()
                .addOnSuccessListener { finish() }
        }
    }

    private fun setFields() {
        val docRef = db.collection("pins").document(id!!)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    pin = Pin(
                        id = document.id,
                        date = document.data?.get("date") as String?,
                        description = document.data?.get("description") as String?,
                        imageUrl = document.data?.get("imageUrl") as String?,
                        latitude = document.data?.get("latitude") as Double?,
                        longitude = document.data?.get("longitude") as Double?,
                        user = document.data?.get("user") as String?
                    )
                }
                val address = getAddress(pin?.latitude!!, pin?.longitude!!)
                Picasso.get().load(pin?.imageUrl).into(ivCamera);
                tvDescription.text = pin?.description
                tvDate.text = pin?.date
                tvLocation.text = address
            }
    }

    // Converts LatLong to address
    private fun getAddress(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this)
        val list = geocoder.getFromLocation(lat, lng, 1)
        return list[0].getAddressLine(0)
    }
}
