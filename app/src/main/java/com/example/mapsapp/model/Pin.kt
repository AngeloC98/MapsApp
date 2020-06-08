package com.example.mapsapp.model

import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class Pin(
    var user: String?,
    var date: String?,
    var latitude: Double?,
    var longitude: Double?,
    var description: String?,
    var imageUrl: String?,
    var id: String? = null

) {
}