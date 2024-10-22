package com.example.commonspotnavigation.utils

import com.mapbox.mapboxsdk.geometry.LatLng

data class RouteData(
    val routePoints: List<LatLng>,
    val durationInSeconds: Int,
    val steps: List<NavigationStep>
)