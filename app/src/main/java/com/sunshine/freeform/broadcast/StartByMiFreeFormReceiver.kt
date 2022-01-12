package com.sunshine.freeform.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunshine.freeform.activity.floating_view.FreeFormWindow

/**
 * @author sunshine
 * @date 2021/3/8
 */
class StartByMiFreeFormReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val activityName = intent?.getStringExtra("activityName")
        val packageName = intent?.getStringExtra("packageName")

        if (activityName != null && packageName != null) {
            FreeFormWindow(
                context!!,
                "am start -n $activityName --display ",
                packageName
            )
            val home = Intent(Intent.ACTION_MAIN)
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            home.addCategory(Intent.CATEGORY_HOME)
            context.startActivity(home)
        }
    }
}