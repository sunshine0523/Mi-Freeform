package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.service.CoreService
import kotlinx.coroutines.DelicateCoroutinesApi


/**
 * @author sunshine
 * @date 2021/3/8
 */
@DelicateCoroutinesApi
class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == action_boot) {
            context.startService(Intent(context, CoreService::class.java))
        }
    }

    companion object {
        const val action_boot = "android.intent.action.BOOT_COMPLETED"
    }
}
