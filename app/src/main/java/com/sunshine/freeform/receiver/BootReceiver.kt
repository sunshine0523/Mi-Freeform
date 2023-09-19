package com.sunshine.freeform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.MiFreeform
import com.sunshine.freeform.service.SidebarService

/**
 * @author KindBrave
 * @since 2023/9/19
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val BOOT = "android.intent.action.BOOT_COMPLETED"
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BOOT) {
            //context.startService(Intent(context, SidebarService::class.java))
        }
    }
}