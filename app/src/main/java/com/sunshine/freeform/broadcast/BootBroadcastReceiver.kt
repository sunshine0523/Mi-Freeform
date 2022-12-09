package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.service.ForegroundService
import com.sunshine.freeform.service.KeepAliveService

/**
 * @author sunshine
 * @date 2021/3/8
 */
class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sp = context.getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
        if (intent.action == action_boot) {
            if (sp.getInt("service_type", KeepAliveService.SERVICE_TYPE) == ForegroundService.SERVICE_TYPE)
                context.startForegroundService(Intent(context, ForegroundService::class.java))
        }
    }

    companion object {
        const val action_boot = "android.intent.action.BOOT_COMPLETED"
    }
}
