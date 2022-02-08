package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.service.CoreService
import com.sunshine.freeform.view.floating.FloatingView
import kotlinx.coroutines.DelicateCoroutinesApi

@DelicateCoroutinesApi
class StartFloatingViewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val showLocation = intent.getIntExtra(CoreService.SHOW_LOCATION, -1)
        FloatingView(context, showLocation)
    }
}