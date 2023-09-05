package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.ui.floating.FloatingActivity
import com.sunshine.freeform.ui.main.LogWidget
import kotlinx.coroutines.DelicateCoroutinesApi
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/27
 */
@DelicateCoroutinesApi
class QuickStartTileService : TileService() {

    override fun onClick() {
        super.onClick()
        startActivity(Intent(this, FloatingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        MiFreeformServiceManager.collapseStatusBar()
    }
}