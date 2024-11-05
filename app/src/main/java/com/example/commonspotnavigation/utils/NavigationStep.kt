package com.example.commonspotnavigation.utils

import com.mapbox.mapboxsdk.geometry.LatLng

data class NavigationStep(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val maneuverLocation: LatLng,
    val maneuverType: String,
    val name: String
)