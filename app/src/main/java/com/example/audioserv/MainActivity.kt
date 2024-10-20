package com.example.audioserv

import android.content.Intent
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import kotlinx.coroutines.delay
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private var logMessages = mutableStateListOf<String>()
    private var ipAddress by mutableStateOf("")
    private var backPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

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
                onRefresh = { isRefreshing = true }
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
}

@Composable
fun AudioServerApp(
    logMessages: List<String>,
    ipAddress: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    MaterialTheme {
        val swipeRefreshState = remember { SwipeRefreshState(isRefreshing) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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