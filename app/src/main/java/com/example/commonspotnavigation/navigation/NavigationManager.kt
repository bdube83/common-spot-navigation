package com.example.commonspotnavigation.navigation

import com.example.commonspotnavigation.utils.NavigationStep
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlin.math.*

object NavigationManager {
    private const val MANEUVER_THRESHOLD = 30.0 // meters

    fun LatLng.distanceTo(other: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(other.latitude - this.latitude)
        val dLon = Math.toRadians(other.longitude - this.longitude)
        val lat1 = Math.toRadians(this.latitude)
        val lat2 = Math.toRadians(other.latitude)

        val a = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun shouldAdvanceToNextStep(
        currentLocation: LatLng,
        nextStep: NavigationStep
    ): Boolean {
        val distanceToManeuver = currentLocation.distanceTo(nextStep.maneuverLocation)
        return distanceToManeuver < MANEUVER_THRESHOLD
    }

    fun getBearingToNextStep(currentLocation: LatLng, nextStepLocation: LatLng): Float {
        val lat1 = Math.toRadians(currentLocation.latitude)
        val lon1 = Math.toRadians(currentLocation.longitude)
        val lat2 = Math.toRadians(nextStepLocation.latitude)
        val lon2 = Math.toRadians(nextStepLocation.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 90) % 90).toFloat() // Normalize bearing to 0-90 degrees
    }
}
