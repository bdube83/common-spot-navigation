package com.example.commonspotnavigation.ui

import android.view.View
import android.view.ViewGroup
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import com.example.commonspotnavigation.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

class CustomInfoWindow(view: View, mapView: MapView) : InfoWindow(view, mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)

        // Remove composeView from its parent if it already has one
        (composeView.parent as? ViewGroup)?.removeView(composeView)

        composeView.setContent {
            InfoContent(
                title = marker.title ?: "",
                description = marker.snippet ?: ""
            )
        }

        // Add composeView to the MapView
        (marker.relatedObject as? MapView)?.addView(composeView)
    }


    override fun onClose() {
        // No additional actions needed on close
    }

    @Composable
    fun InfoContent(title: String, description: String) {
        Surface(
            color = Color.White,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    fun addRouteLine(mapView: MapView, startPoint: GeoPoint, endPoint: GeoPoint) {
        val roadOverlay = Polyline().apply {
            setPoints(listOf(startPoint, endPoint))
            color = Color.Blue.toArgb()
            width = 10f
        }
        mapView.overlays.add(roadOverlay)
    }
}
