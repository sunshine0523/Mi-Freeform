package io.sunshine0523.freeform.ui.freeform

import android.app.PendingIntent
import android.content.ComponentName

data class AppConfig(
    val packageName: String,
    val activityName: String,
    val pendingIntent: PendingIntent?,
    val userId: Int
)