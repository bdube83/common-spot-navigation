package com.example.commonspotnavigation.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import com.example.commonspotnavigation.R
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(context: Context, currentLocation: GeoPoint) {
    var startPoint by remember { mutableStateOf(currentLocation) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endAddress by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // MapView placed in the background
        AndroidView(factory = {
            val mapView = MapView(context)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(startPoint)

            // Function to add markers and route line
            fun updateMap() {
                mapView.overlays.clear()

                // Create start marker (current location)
                val startMarker = Marker(mapView).apply {
                    position = startPoint
                    title = "Current Location"
                    snippet = "This is your current location"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // Set custom icon for the marker
                    icon = context.getDrawable(R.drawable.ic_current_location_marker) // Replace with your drawable resource

                    // Set the custom info window
                    val infoWindowView = ComposeView(context)
                    infoWindowView.id = R.id.compose_view // Ensure to have an ID for finding the view later
                    mapView.addView(infoWindowView)

                    infoWindow = CustomInfoWindow(infoWindowView, mapView)
                }

                // Create end marker if available
                endPoint?.let { endPoint ->
                    val endMarker = Marker(mapView).apply {
                        position = endPoint
                        title = "Destination"
                        snippet = "This is the destination"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        // Set custom icon for the marker
                        icon = context.getDrawable(R.drawable.ic_destination_marker) // Replace with your drawable resource

                        // Set the custom info window
                        val infoWindowView = ComposeView(context)
                        infoWindowView.id = R.id.compose_view // Ensure to have an ID for finding the view later
                        mapView.addView(infoWindowView)

                        infoWindow = CustomInfoWindow(infoWindowView, mapView)
                    }

                    // Add markers to the map
                    mapView.overlays.add(endMarker)

                    // Add route line between start and end point
                    val routeLine = Polyline().apply {
                        setPoints(listOf(startPoint, endPoint))
                        color = context.getColor(R.color.purple_700) // Use a color similar to the screenshot (replace with your color resource)
                        width = 8.0f // Set line width
                    }
                    mapView.overlays.add(routeLine)
                }

                // Always add the current location marker
                mapView.overlays.add(startMarker)

                mapView.invalidate() // Refresh the map
            }

            // Update the map initially
            updateMap()

            mapView
        }, update = { mapView ->
            // Update the map whenever the end point changes
            mapView.controller.setCenter(startPoint)
            mapView.overlays.clear()

            val startMarker = Marker(mapView).apply {
                position = startPoint
                title = "Current Location"
                snippet = "This is your current location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Set custom icon for the marker
                icon = context.getDrawable(R.drawable.ic_current_location_marker) // Replace with your drawable resource

                // Set the custom info window
                val infoWindowView = ComposeView(context)
                infoWindowView.id = R.id.compose_view
                mapView.addView(infoWindowView)

                infoWindow = CustomInfoWindow(infoWindowView, mapView)
            }

            // Add the end marker if available
            endPoint?.let { endPoint ->
                val endMarker = Marker(mapView).apply {
                    position = endPoint
                    title = "Destination"
                    snippet = "This is the destination"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // Set custom icon for the marker
                    icon = context.getDrawable(R.drawable.ic_destination_marker) // Replace with your drawable resource

                    // Set the custom info window
                    val infoWindowView = ComposeView(context)
                    infoWindowView.id = R.id.compose_view
                    mapView.addView(infoWindowView)

                    infoWindow = CustomInfoWindow(infoWindowView, mapView)
                }

                mapView.overlays.add(endMarker)

                val routeLine = Polyline().apply {
                    setPoints(listOf(startPoint, endPoint))
                    color = context.getColor(R.color.purple_700) // Use a color similar to the screenshot (replace with your color resource)
                    width = 8.0f // Set line width
                }
                mapView.overlays.add(routeLine)
            }

            mapView.overlays.add(startMarker)

            mapView.invalidate()
        },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        // Column containing input field and button placed in the foreground
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .zIndex(1f) // Ensure the inputs are on top of the map
                .align(Alignment.TopCenter)
        ) {
            TextField(
                value = endAddress,
                onValueChange = { endAddress = it },
                label = { Text("End Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        // Simulated Geocoding Function to convert address to GeoPoint
                        val newEndPoint = geocodeAddress(endAddress)

                        // Update the end point
                        if (newEndPoint != null) {
                            endPoint = newEndPoint
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Update Route")
            }
        }
    }
}

// Placeholder geocoding function to simulate converting an address to a GeoPoint
suspend fun geocodeAddress(address: String): GeoPoint? {
    return withContext(Dispatchers.IO) {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)

        // In a real application, you'd perform a network request to a geocoding API here
        // Example: Using OpenStreetMap's Nominatim or Google Maps Geocoding API

        // For simulation, we'll return a dummy GeoPoint based on the address string
        when (address.lowercase()) {
            "cape town" -> GeoPoint(-33.9249, 18.4241) // Example for Cape Town
            "jhb", "johannesburg" -> GeoPoint(-26.2041, 28.0473) // Example for Johannesburg
            "san francisco" -> GeoPoint(37.7749, -122.4194) // Example for San Francisco
            "los angeles" -> GeoPoint(34.0522, -118.2437) // Example for Los Angeles
            else -> null // Return null if address is not recognized
        }
    }
}
