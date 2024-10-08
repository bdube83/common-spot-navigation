package com.example.commonspotnavigation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.commonspotnavigation.location.LocationProvider
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen() {
    val context = LocalContext.current

    // State for current location
    var geoPoint by remember { mutableStateOf(GeoPoint(48.8583, 2.2944)) } // Default to Eiffel Tower
    val locationProvider = remember { LocationProvider(context) { location ->
        geoPoint = GeoPoint(location.latitude, location.longitude)
    }}

    LaunchedEffect(Unit) {
        // Start location updates
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationProvider.startLocationUpdates()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Stop location updates when the composable is disposed
            locationProvider.stopLocationUpdates()
        }
    }

    // Map rendering
    AndroidView(factory = { MapView(context).apply {
        setMultiTouchControls(true)
        controller.setZoom(15.0)
        controller.setCenter(geoPoint)

        val marker = Marker(this).apply {
            position = geoPoint
            title = "Driver's Location"
        }
        overlays.add(marker)
    }}, update = { mapView ->
        // Update the map on location change
        mapView.controller.setCenter(geoPoint)

        mapView.overlays.clear()
        val marker = Marker(mapView).apply {
            position = geoPoint
            title = "Driver's Location"
        }
        mapView.overlays.add(marker)
    })
}
