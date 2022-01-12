package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sunshine.freeform.callback.SuiServerListener
import com.sunshine.freeform.service.FloatingService
import com.sunshine.freeform.service.ForegroundService
import com.sunshine.freeform.service.NotificationService
import com.sunshine.freeform.utils.FreeFormUtils


/**
 * @author sunshine
 * @date 2021/3/8
 */
class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sp = context.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
        if (intent.action == action_boot) {
            if (sp.getBoolean("switch_foreground_service", false)) context.startForegroundService(Intent(context, ForegroundService::class.java))
            FreeFormUtils.init(object : SuiServerListener() {
                override fun onStart() {
                    Toast.makeText(context, "米窗已经开机自启", Toast.LENGTH_SHORT).show()
                }

                override fun onStop() {

                }

            })
            if (sp.getBoolean("switch_floating", false)) {
                context.startService(Intent(context, FloatingService::class.java))
            }

            if (sp.getBoolean("switch_notify", false)) {
                context.startService(Intent(context, NotificationService::class.java))
            }
        }
    }

    companion object {
        const val action_boot = "android.intent.action.BOOT_COMPLETED"
    }
}
