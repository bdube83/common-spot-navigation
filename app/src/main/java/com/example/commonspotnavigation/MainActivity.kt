package com.example.commonspotnavigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.commonspotnavigation.location.LocationProvider
import com.example.commonspotnavigation.ui.MapScreen
import com.example.commonspotnavigation.ui.theme.CommonSpotNavigationTheme
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MainActivity : ComponentActivity() {

    private lateinit var locationProvider: LocationProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid to use this application's preferences
        val osmConfig = Configuration.getInstance()
        osmConfig.load(applicationContext, getPreferences(MODE_PRIVATE))
        osmConfig.userAgentValue = packageName // Use your package name as the user agent

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
            // Update the map screen with the new location
            val currentLocation = GeoPoint(location.latitude, location.longitude)
            setContent {
                CommonSpotNavigationTheme {
                    MapScreen(
                        context = this,
                        currentLocation,
                    )
                }
            }
        }
        locationProvider.startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationProvider.stopLocationUpdates()
    }
}