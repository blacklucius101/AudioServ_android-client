package com.example.audioserv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import kotlinx.coroutines.delay
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private var logMessages = mutableStateListOf<String>()
    private var ipAddress by mutableStateOf("")
    private var backPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())
    private var showTutorialText by mutableStateOf(true)  // State to show tutorial text

    companion object {
        private const val REQUEST_CODE_FOREGROUND_SERVICE_MEDIA_PROJECTION = 100
    }

    override fun onStart() {
        super.onStart()

        // Check for normal permissions (INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE)
        checkAndNotifyPermission(Manifest.permission.FOREGROUND_SERVICE)
        checkAndNotifyPermission(Manifest.permission.INTERNET)
        checkAndNotifyPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        checkAndNotifyPermission(Manifest.permission.ACCESS_WIFI_STATE)
        checkAndNotifyPermission(Manifest.permission.CHANGE_WIFI_STATE)
    }

    // Helper function to check and notify permission status for normal permissions
    private fun checkAndNotifyPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "$permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_FOREGROUND_SERVICE_MEDIA_PROJECTION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with the functionality
                    Toast.makeText(this, "Permission granted for Foreground Service Media Projection", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied, handle accordingly
                    Toast.makeText(this, "Permission denied for Foreground Service Media Projection", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Handle other permissions results if needed
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the IP address
        ipAddress = getIPAddress()

        // Start the foreground service
        val serviceIntent = Intent(this, AudioService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            var isRefreshing by remember { mutableStateOf(false) }

            LaunchedEffect(isRefreshing) {
                if (isRefreshing) {
                    ipAddress = getIPAddress()
                    logMessages.add("IP Address refreshed: $ipAddress")
                    delay(1000) // Simulate a network call
                    isRefreshing = false
                }
            }

            AudioServerApp(
                logMessages = logMessages,
                ipAddress = ipAddress,
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                showTutorialText = showTutorialText // Pass the tutorial text state
            )
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    // Stop the foreground service
                    val serviceIntent = Intent(this@MainActivity, AudioService::class.java)
                    stopService(serviceIntent)

                    // Finish the activity
                    finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()

                    // Reset the flag after 2 seconds
                    handler.postDelayed({ backPressedOnce = false }, 2000)
                }
            }
        })
    }

    private fun getIPAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            logMessages.add("Error getting IP address: ${ex.message}")
            ex.printStackTrace()
        }
        return "Unavailable"
    }

    fun hideTutorialText() {
        showTutorialText = false  // Hide the tutorial text once back is pressed twice
    }
}

@Composable
fun AudioServerApp(
    logMessages: List<String>,
    ipAddress: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    showTutorialText: Boolean  // Receive the state for showing tutorial text
) {
    MaterialTheme {
        val swipeRefreshState = remember { SwipeRefreshState(isRefreshing) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (showTutorialText) {
                Text(
                    text = "◉ Swipe down to refresh IP Address!",
                    fontSize = 10.sp
                )
                Text(
                    text = "◉ Press back twice to exit the app!",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                Text(
                    text = "ⓘ This app requires VB cable to run. Install the free version and set it as the default audio driver for the PC server. Check for tutorial on YouTube.",
                    fontSize = 5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            Text("Logs", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
            Text("IP Address: $ipAddress", fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = onRefresh
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logMessages.size) { index ->
                            Text(logMessages[index], fontSize = 14.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}
