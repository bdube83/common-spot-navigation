package com.example.commonspotnavigation.utils

import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun geocodeAddress(address: String): LatLng? {
    return withContext(Dispatchers.IO) {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)

        // Dummy coordinates based on the address
        when (address.lowercase().trim()) {
            "cape town" -> LatLng(-33.9249, 18.4241)
            "johannesburg" -> LatLng(-26.2041, 28.0473)
            "san francisco" -> LatLng(37.7749, -122.4194)
            "los angeles" -> LatLng(34.0522, -118.2437)
            "new york" -> LatLng(40.7128, -74.0060)
            "home" -> LatLng(-33.8041137,18.5189693)
            "milnerton" -> LatLng(-33.866669, 18.500000)
            "golf" -> LatLng(-33.716879, 18.449247)
            "gym" -> LatLng(-33.8172284, 18.4981205)

            // Add more addresses as needed
            else -> null // Return null if address is not recognized
        }
    }
}
