package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.utils.FreeFormUtils


class StartActivityReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "StartActivityReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName")
        if (packageName != null) {
            FreeFormUtils.startActivityForHook(packageName)
        }
    }
}