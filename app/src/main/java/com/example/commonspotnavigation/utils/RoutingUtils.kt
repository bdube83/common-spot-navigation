package com.example.commonspotnavigation.utils

import android.util.Log
import com.mapbox.mapboxsdk.annotations.Polyline
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object OSRMRouteHelper {

    private val client = OkHttpClient()

    // OSRM URL for routing requests
    private const val OSRM_URL = "https://router.project-osrm.org/route/v1/driving"

    // Fetch route data from OSRM server
    suspend fun getRoute(start: LatLng, end: LatLng): RouteData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$OSRM_URL/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson&steps=true"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        parseRouteAndDuration(responseData)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseRouteAndDuration(responseData: String): RouteData? {
        return try {
            val json = JSONObject(responseData)
            val routes = json.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val geometry = route.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val duration = route.getDouble("duration").toInt()

                // Parse coordinates
                val routePoints = mutableListOf<LatLng>()
                for (i in 0 until coordinates.length()) {
                    val point = coordinates.getJSONArray(i)
                    val lon = point.getDouble(0)
                    val lat = point.getDouble(1)
                    routePoints.add(LatLng(lat, lon))
                }

                // Parse steps
                val legs = route.getJSONArray("legs")
                val stepsList = mutableListOf<NavigationStep>()
                for (i in 0 until legs.length()) {
                    val leg = legs.getJSONObject(i)
                    val steps = leg.getJSONArray("steps")
                    for (j in 0 until steps.length()) {
                        val step = steps.getJSONObject(j)
                        val distance = step.getDouble("distance")
                        val durationStep = step.getDouble("duration")

                        val maneuver = step.getJSONObject("maneuver")
                        val locationArray = maneuver.optJSONArray("location")
                        if (locationArray != null && locationArray.length() == 2) {
                            val lon = locationArray.getDouble(0)
                            val lat = locationArray.getDouble(1)
                            val maneuverLocation = LatLng(lat, lon)

                            val maneuverType = maneuver.getString("type")
                            val maneuverModifier = maneuver.optString("modifier", "")
                            val name = step.optString("name", "")

                            // Generate instruction
                            val instruction = generateInstruction(maneuverType, maneuverModifier, name)

                            stepsList.add(
                                NavigationStep(
                                    distance = distance,
                                    duration = durationStep,
                                    instruction = instruction,
                                    maneuverLocation = maneuverLocation,
                                    maneuverType = maneuverType,
                                    name = name
                                )
                            )
                        } else {
                            Log.e("OSRMRouteHelper", "Maneuver location is missing or invalid at step $j")
                            continue // Skip this step
                        }
                    }
                }

                RouteData(routePoints = routePoints, durationInSeconds = duration, steps = stepsList)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("OSRMRouteHelper", "Error parsing route: ${e.message}")
            e.printStackTrace()
            null
        }
    }


    suspend fun drawRoute(mapboxMap: MapboxMap, routePoints: List<LatLng>): Polyline? {
        return withContext(Dispatchers.Main) {
            mapboxMap.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(android.graphics.Color.BLUE)
                    .width(5f)
            )
        }
    }

    private fun generateInstruction(type: String, modifier: String, name: String): String {
        val action = when (type) {
            "turn" -> "Turn ${modifier.toLowerCase()}"
            "depart" -> "Start"
            "arrive" -> "You have arrived"
            "merge" -> "Merge"
            "on ramp" -> "Take the ramp"
            "off ramp" -> "Take the exit"
            "fork" -> "Keep ${modifier.toLowerCase()}"
            "end of road" -> "At the end of the road, turn ${modifier.toLowerCase()}"
            "continue" -> "Continue"
            else -> type.replace("_", " ").capitalize()
        }

        val roadName = if (name.isNotBlank()) " onto $name" else ""
        return "$action$roadName"
    }
}