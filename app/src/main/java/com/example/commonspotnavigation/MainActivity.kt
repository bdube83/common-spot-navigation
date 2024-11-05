package com.example.commonspotnavigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.commonspotnavigation.location.LocationProvider
import com.example.commonspotnavigation.ui.MapScreen
import com.example.commonspotnavigation.ui.theme.CommonSpotNavigationTheme
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.geometry.LatLng

class MainActivity : ComponentActivity() {

    private lateinit var locationProvider: LocationProvider

    private var currentLatitude by mutableStateOf(0.0)
    private var currentLongitude by mutableStateOf(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Mapbox with the API key
        Mapbox.getInstance(this, "", WellKnownTileServer.MapTiler)

        // Set up the content and theme for the activity
        setContent {
            CommonSpotNavigationTheme {
                // Show the map and related UI
                if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                    MapScreen(
                        context = this,
                        currentLocation = LatLng(currentLatitude, currentLongitude)
                    )
                } else {
                    // Handle case where location is not yet available
                }
            }
        }

        // Request location permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startLocationProvider()
        }
    }

    // Callback for the permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationProvider()
        }
    }

    private fun startLocationProvider() {
        locationProvider = LocationProvider(this) { location ->
            // Update the currentLatitude and currentLongitude when a new location is received
            currentLatitude = location.latitude
            currentLongitude = location.longitude
        }
        locationProvider.startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationProvider.stopLocationUpdates()
    }
}
