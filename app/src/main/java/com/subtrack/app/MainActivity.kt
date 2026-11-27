package com.subtrack.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.app.ui.navigation.AppViewModel
import com.subtrack.app.ui.navigation.SubTrackApp
import com.subtrack.core.domain.backup.SubscriptionBackupCodec
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.ui.theme.SubTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openSubscriptionId by mutableStateOf<Long?>(null)
    private var pendingTransferPayload by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Handled reactively when returning to app / Settings.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            LaunchedEffect(useDarkTheme) {
                // On Android 8.0-9 (API 26-28, notably Huawei EMUI 8.0), a pure 0x00000000
                // status bar color can prevent OEM dark-icon luminance detection in Light mode.
                // Using a whisper-subtle ~5% alabaster scrim below API 29 plus explicitly syncing
                // WindowInsetsControllerCompat guarantees readable status & nav bar glyphs.
                val lightStatusScrim = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Color.argb(0x0D, 0xF5, 0xF6, 0xF5)
                } else {
                    Color.TRANSPARENT
                }
                val darkFallbackScrim = Color.argb(0x66, 0x10, 0x12, 0x13)
                val statusStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(lightStatusScrim, darkFallbackScrim)
                }
                val navStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, darkFallbackScrim)
                }
                enableEdgeToEdge(
                    statusBarStyle = statusStyle,
                    navigationBarStyle = navStyle
                )
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }

            SubTrackTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SubTrackApp(
                        onRequestNotificationPermission = ::checkAndRequestNotificationPermission,
                        openSubscriptionId = openSubscriptionId,
                        onOpenSubscriptionConsumed = { openSubscriptionId = null },
                        pendingTransferPayload = pendingTransferPayload,
                        onTransferPayloadConsumed = { pendingTransferPayload = null },
                        appViewModel = appViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) return
        incoming.calendarSubscriptionId()?.let { openSubscriptionId = it }

        when (incoming.action) {
            Intent.ACTION_VIEW -> {
                val uri = incoming.data ?: return
                if (uri.scheme.equals(SubscriptionBackupCodec.DEEP_LINK_SCHEME, ignoreCase = true)) {
                    pendingTransferPayload = uri.toString()
                } else if (uri.scheme.equals("content", ignoreCase = true)) {
                    readIncomingTransferUri(uri)
                }
            }
            Intent.ACTION_SEND -> {
                val sharedText = incoming.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    pendingTransferPayload = sharedText
                    return
                }
                val streamUri = IntentCompat.getParcelableExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)
                if (streamUri != null) {
                    readIncomingTransferUri(streamUri)
                }
            }
        }
    }

    private fun readIncomingTransferUri(uri: Uri) {
        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val maxBytes = 512 * 1024
                        val buffer = ByteArray(4096)
                        val out = java.io.ByteArrayOutputStream()
                        var total = 0
                        while (true) {
                            val read = stream.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > maxBytes) error("File too large")
                            out.write(buffer, 0, read)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }
                }.getOrNull()
            }
            if (!content.isNullOrBlank()) {
                pendingTransferPayload = content
            }
        }
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
