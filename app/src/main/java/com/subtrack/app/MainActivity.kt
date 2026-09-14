package com.subtrack.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.subtrack.app.ui.navigation.SubTrackApp
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.core.ui.theme.SubTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openSubscriptionId by mutableStateOf<Long?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openSubscriptionId = intent.calendarSubscriptionId()

        setContent {
            SubTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SubTrackApp(
                        onRequestNotificationPermission = ::checkAndRequestNotificationPermission,
                        openSubscriptionId = openSubscriptionId,
                        onOpenSubscriptionConsumed = { openSubscriptionId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openSubscriptionId = intent.calendarSubscriptionId()
    }

    private fun Intent.calendarSubscriptionId(): Long? =
        getLongExtra(NotificationHelper.EXTRA_OPEN_SUBSCRIPTION_ID, Long.MIN_VALUE)
            .takeUnless { it == Long.MIN_VALUE }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
