package com.example.commonspotnavigation.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.launch
import com.example.commonspotnavigation.utils.geocodeAddress
import CustomInfoWindow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.commonspotnavigation.R
import com.example.commonspotnavigation.navigation.NavigationManager
import com.example.commonspotnavigation.utils.NavigationStep
import com.example.commonspotnavigation.utils.OSRMRouteHelper
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.annotations.Polyline
import java.lang.Math.*
import kotlin.math.pow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(context: Context, currentLocation: LatLng) {
    var endAddress by remember { mutableStateOf("") }
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
    val mapView = remember { MapView(context) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var destinationPoint by remember { mutableStateOf<LatLng?>(null) }
    var estimatedTravelTime by remember { mutableStateOf<String?>(null) }
    var currentRouteLine by remember { mutableStateOf<Polyline?>(null) }
    var navigationSteps by remember { mutableStateOf<List<NavigationStep>>(emptyList()) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var isNavigationActive by remember { mutableStateOf(false) }


    suspend fun drawRoute(
        map: MapboxMap,
        origin: LatLng,
        destination: LatLng
    ): Polyline? {
        // Clear the previous route line if it exists
        currentRouteLine?.remove()

        // Get the route points and travel time from OSRM
        val routeData = OSRMRouteHelper.getRoute(origin, destination)

        return if (routeData != null) {
            val (routePoints, durationInSeconds, steps) = routeData

            // Update navigation steps
                navigationSteps = steps
            currentStepIndex = 0 // Start from the first step

            // Draw the new route line
            val newRouteLine = OSRMRouteHelper.drawRoute(map, routePoints)
            currentRouteLine = newRouteLine  // Update the currentRouteLine reference

            // Format the estimated travel time
            val travelTime = if (durationInSeconds < 3600) {
                "${durationInSeconds / 60} minutes"
            } else {
                "${durationInSeconds / 3600} hours ${durationInSeconds % 3600 / 60} minutes"
            }

            // Update the state variable with the estimated travel time
            estimatedTravelTime = travelTime

            // Fit the camera to the route bounds
            val boundsBuilder = LatLngBounds.Builder()
            for (point in routePoints) {
                boundsBuilder.include(point)
            }
            val bounds = boundsBuilder.build()
            if (!isNavigationActive) {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }

            newRouteLine
        } else {
            // Handle no route found
            estimatedTravelTime = null  // Reset the travel time
            null
        }
    }

    // Manage MapView lifecycle
    DisposableEffect(key1 = mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()

        mapView.getMapAsync { mapboxMap ->
            mapboxMap.uiSettings.isLogoEnabled = false
            mapboxMap.uiSettings.isAttributionEnabled = false
        }

        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // MapView placed in the background
        AndroidView(factory = {
            mapView.apply {
                getMapAsync { mapboxMapLoaded ->
                    val apiKey = ""
                    val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"

                    mapboxMapLoaded.setStyle(styleUrl) { style ->
                        // Map is ready
                        mapboxMap = mapboxMapLoaded
                    }
                }
            }
        },
            update = {
                mapboxMap?.let { map ->
                    if (currentLocation.latitude != 0.0 && currentLocation.longitude != 0.0) {

                        val bearing = if (isNavigationActive && navigationSteps.isNotEmpty()) {
                            NavigationManager.getBearingToNextStep(currentLocation, navigationSteps[currentStepIndex].maneuverLocation)
                        } else {
                            0.0
                        }

                        val position = CameraPosition.Builder()
                            .target(currentLocation)
                            .zoom(if (isNavigationActive) 18.0 else 15.0)
                            .tilt(if (isNavigationActive) 45.0 else 0.0)
                            //.bearing(bearing.toDouble())
                            .build()
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(position))

                        map.clear()

                        // Add current location marker
                        val customInfoWindow = CustomInfoWindow(context, mapView, map)

                        customInfoWindow.displayMarkerInfo(
                            "Current Location",
                            "Your current location",
                            currentLocation.latitude,
                            currentLocation.longitude,
                            if (isNavigationActive) R.drawable.ic_current_location_marker else R.drawable.ic_current_location_marker
                        )

                        // Draw route and add destination marker if destination is set
                        destinationPoint?.let { destPoint ->
                            coroutineScope.launch {
                                drawRoute(map, currentLocation, destPoint)
                            }
                            val destinationInfoWindow = CustomInfoWindow(context, mapView, map)
                            destinationInfoWindow.displayMarkerInfo(
                                "Destination",
                                endAddress,
                                destPoint.latitude,
                                destPoint.longitude
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        Text(
            text = "CommonLink ©",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(4.dp),
            style = MaterialTheme.typography.bodySmall
        )

        // Foreground UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .zIndex(1f)
                .align(Alignment.TopCenter)
        ) {
            if (!isNavigationActive) {
                TextField(
                    value = endAddress,
                    onValueChange = {
                        // Existing code with navigation reset
                        endAddress = it

                        // Reset destination and route when user starts typing
                        destinationPoint = null
                        currentRouteLine?.remove()
                        currentRouteLine = null
                        estimatedTravelTime = null
                        navigationSteps = emptyList()
                        isNavigationActive = false // Reset navigation state
                        currentStepIndex = 0

                        // Move camera back to the current location
                        mapboxMap?.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(currentLocation)
                                    .zoom(15.0)
                                    .build()
                            )
                        )
                    },
                    label = { Text("Where to?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                if (estimatedTravelTime === null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Existing code to fetch route
                                isLoading = true
                                errorMessage = null
                                estimatedTravelTime = null  // Reset estimated travel time

                                val inputAddress = endAddress.trim()

                                if (inputAddress.isBlank()) {
                                    errorMessage = "Please enter an address."
                                    isLoading = false
                                    return@launch
                                }

                                val newEndPoint = geocodeAddress(inputAddress)
                                isLoading = false
                                if (newEndPoint != null) {
                                    destinationPoint = newEndPoint
                                    mapboxMap?.let { map ->
                                        map.clear()

                                        // Add marker at destination
                                        val customInfoWindow =
                                            CustomInfoWindow(context, mapView, map)
                                        customInfoWindow.displayMarkerInfo(
                                            "Destination",
                                            endAddress,
                                            newEndPoint.latitude,
                                            newEndPoint.longitude
                                        )

                                        // Draw route from current location to destination
                                        drawRoute(map, currentLocation, newEndPoint)
                                    }
                                } else {
                                    errorMessage = "Address not recognized."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Go")
                        }
                    }
                }
            }

            // Display error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Display estimated travel time
            if (estimatedTravelTime != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Estimated travel time: $estimatedTravelTime",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (estimatedTravelTime != null && !isNavigationActive) {
                Button(
                    onClick = {
                        isNavigationActive = true
                        currentStepIndex = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Start Navigation")
                }
            }

            // Navigation UI
            if (isNavigationActive) {
                // Display current instruction
                if (navigationSteps.isNotEmpty() && currentStepIndex < navigationSteps.size) {
                    val currentInstruction = navigationSteps[currentStepIndex].instruction
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = currentInstruction,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    // Navigation completed
                    Text(
                        text = "You have arrived at your destination.",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Navigation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            // Implement pause functionality if needed
                        },
                        enabled = false, // Set to true if implementing pause
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Pause")
                    }
                    Button(
                        onClick = {
                            // Cancel navigation
                            isNavigationActive = false
                            currentStepIndex = 0
                            navigationSteps = emptyList()
                            estimatedTravelTime = null
                            endAddress = ""
                            destinationPoint = null
                            currentRouteLine?.remove()
                            currentRouteLine = null

                            // Move camera back to the current location
                            mapboxMap?.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(currentLocation)
                                        .zoom(15.0)
                                        .build()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Cancel Navigation")
                    }
                }
            }


        }
    }

    LaunchedEffect(currentLocation) {
        if (isNavigationActive && navigationSteps.isNotEmpty() && currentStepIndex < navigationSteps.size) {
            val nextStep = navigationSteps[currentStepIndex]
            val shouldAdvance = NavigationManager.shouldAdvanceToNextStep(currentLocation, nextStep)
            if (shouldAdvance) {
                currentStepIndex++
            }
        }
    }

}
