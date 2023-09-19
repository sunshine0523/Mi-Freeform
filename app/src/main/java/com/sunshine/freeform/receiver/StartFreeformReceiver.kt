package com.sunshine.freeform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.MiFreeform
import com.sunshine.freeform.MiFreeformServiceManager
import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/9/19
 */
class StartFreeformReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION = "com.sunshine.freeform.start_freeform"
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
            val packageName = intent.getStringExtra("packageName")
            val activityName = intent.getStringExtra("activityName")
            val userId = intent.getIntExtra("userId", 0)

            if (packageName != null && activityName != null) {
                val sp = context.getSharedPreferences(MiFreeform.CONFIG, Context.MODE_PRIVATE)
                val screenWidth = context.resources.displayMetrics.widthPixels
                val screenHeight = context.resources.displayMetrics.heightPixels
                val screenDensityDpi = context.resources.displayMetrics.densityDpi
                MiFreeformServiceManager.createWindow(
                    packageName,
                    activityName,
                    userId,
                    sp.getInt("freeform_width", (screenWidth * 0.8).roundToInt()),
                    sp.getInt("freeform_height", (screenHeight * 0.5).roundToInt()),
                    sp.getInt("freeform_dpi", screenDensityDpi),
                )
            }
        }
    }
}