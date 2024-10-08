package com.example.commonspotnavigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.commonspotnavigation.ui.MapScreen
import com.example.commonspotnavigation.ui.theme.CommonSpotNavigationTheme
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid to use this application's preferences
        val osmConfig = Configuration.getInstance()
        osmConfig.load(applicationContext, getPreferences(MODE_PRIVATE))
        osmConfig.userAgentValue = packageName // Use your package name as the user agent

        setContent {
            CommonSpotNavigationTheme {
                MapScreen() // Set up the map screen UI
            }
        }
    }
}
