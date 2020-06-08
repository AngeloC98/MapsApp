package com.example.mapsapp.model

import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class Pin(
    var user: String,
    var date: LocalDateTime,
    var location: LatLng,
    var description: String,
    var imageUrl: String

) {
}