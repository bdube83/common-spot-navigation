import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import androidx.compose.ui.platform.ComposeView
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import androidx.appcompat.content.res.AppCompatResources

class CustomInfoWindow(
    private val context: Context,
    private val mapView: MapView,
    private val mapboxMap: MapboxMap
) {
    private var isInfoWindowVisible by mutableStateOf(false)
    private var currentPopupView: ComposeView? = null

    fun displayMarkerInfo(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        iconResId: Int? = null
    ) {
        val icon: Icon? = iconResId?.let { resId ->
            val drawable = AppCompatResources.getDrawable(context, resId)
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                // If drawable is a vector or non-bitmap, convert it to a bitmap
                val bitmap = Bitmap.createBitmap(
                    drawable!!.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            IconFactory.getInstance(context).fromBitmap(bitmap)
        }

        // Create marker with custom icon if available
        val markerOptions = MarkerOptions()
            .position(LatLng(latitude, longitude))
            .title(title)
            .snippet(description)

        if (icon != null) {
            markerOptions.icon(icon)
        } else {
            println("Warning: Icon could not be created for resource ID $iconResId")
        }

        val marker = mapboxMap.addMarker(markerOptions)

        // Set up custom info window handling and camera movement listeners as before
        mapboxMap.setInfoWindowAdapter { null }
        mapboxMap.addOnCameraMoveListener { updatePopupPosition(marker) }
        mapboxMap.setOnMarkerClickListener { clickedMarker ->
            if (clickedMarker == marker) {
                isInfoWindowVisible = true
                showPopup(clickedMarker)
                true
            } else {
                false
            }
        }
    }

    private fun showPopup(marker: Marker) {
        // Remove the previous popup view if one exists
        currentPopupView?.let { mapView.removeView(it) }

        // Add new popup view
        currentPopupView = ComposeView(context).apply {
            setContent {
                if (isInfoWindowVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isInfoWindowVisible = false } // Dismiss popup when clicking outside
                    ) {
                        val markerScreenPosition = mapboxMap.projection.toScreenLocation(marker.position)
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(
                                markerScreenPosition.x.toInt() - 300,
                                markerScreenPosition.y.toInt()- 400
                            ),
                            onDismissRequest = { isInfoWindowVisible = false }
                        ) {
                            InfoContent(
                                title = marker.title ?: "Unknown",
                                description = marker.snippet ?: "No details",
                                onDismiss = { isInfoWindowVisible = false }
                            )
                        }
                    }
                }
            }
        }
        mapView.addView(currentPopupView)
    }

    private fun updatePopupPosition(marker: Marker) {
        if (isInfoWindowVisible) {
            val markerScreenPosition = mapboxMap.projection.toScreenLocation(marker.position)
            currentPopupView?.let {
                it.x = markerScreenPosition.x - (it.width / 2)
                it.y = markerScreenPosition.y - it.height - 100 // Adjust y to place popup above marker
            }
        }
    }

    @Composable
    fun InfoContent(title: String, description: String, onDismiss: () -> Unit) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp)
                .clickable { onDismiss() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    }
}
