package com.subtrack.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subtrack.app.SubTrackApplication
import com.subtrack.app.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val app = context.applicationContext as? SubTrackApplication ?: return
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val subs = app.repository.getAllSubscriptionsList()
                subs.forEach { sub ->
                    AlarmScheduler.scheduleSubscriptionAlert(context, sub)
                }
            }
        }
    }
}
