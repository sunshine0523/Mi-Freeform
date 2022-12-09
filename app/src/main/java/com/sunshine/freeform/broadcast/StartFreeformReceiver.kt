package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.ui.freeform.FreeformConfig
import com.sunshine.freeform.ui.freeform.FreeformView

class StartFreeformReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName")
        val activityName = intent.getStringExtra("activityName")
        val userId = intent.getIntExtra("userId", 0)
        val extras = intent.getStringExtra("extras")

        if (packageName != null && activityName != null) {
            FreeformView(
                FreeformConfig(
                    useCustomConfig = false,
                    packageName = packageName,
                    activityName = activityName,
                    userId = userId
                ),
                context
            )
        }
    }
}