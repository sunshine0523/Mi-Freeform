package com.sunshine.freeform.service.tile

import android.content.Context
import android.content.res.Configuration
import android.service.quicksettings.TileService
import android.view.WindowManager
import android.widget.Toast
import com.sunshine.freeform.R
import com.sunshine.freeform.service.core.CoreService
import com.sunshine.freeform.service.floating.FreeFormAppsFloatingView

/**
 * @author sunshine
 * @date 2021/2/27
 * 通知栏快捷磁贴服务
 */
class QuickStartTileService : TileService() {

    private var freeFormAppsFloatingView: FreeFormAppsFloatingView? = null
    private var orientation: Int = Configuration.ORIENTATION_UNDEFINED

    override fun onClick() {
        super.onClick()
        if (CoreService.isRunning) {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
            freeFormAppsFloatingView = FreeFormAppsFloatingView(this, null, windowManager, sp, CoreService.floatingApps)
            freeFormAppsFloatingView!!.showFloatingViewWindow()
        } else {
            Toast.makeText(this, getString(R.string.core_not_running), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation != orientation) {
            orientation = newConfig.orientation
            freeFormAppsFloatingView?.removeFloatingViewWindows()
            freeFormAppsFloatingView?.showFloatingViewWindow()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}