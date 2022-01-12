package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.service.quicksettings.TileService
import android.view.WindowManager
import android.widget.Toast
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.floating_view.FreeFormAppsFloatingView
import com.sunshine.freeform.callback.SuiServerListener
import com.sunshine.freeform.utils.FreeFormUtils
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/27
 * 通知栏快捷磁贴服务
 */
class QuickStartTileService : TileService() {

    //private var freeFormAppsFloatingView: FreeFormAppsFloatingView? = null
    private var orientation: Int = Configuration.ORIENTATION_UNDEFINED

    override fun onClick() {
        super.onClick()

        if (FreeFormUtils.getControlService() != null && FreeFormUtils.serviceInitSuccess()) {
            //需要--activity-clear-task防止前台被打开
            FreeFormUtils.getControlService()!!.startActivity("am start -n com.sunshine.freeform/.activity.floating_view.FloatingViewActivity --activity-clear-task")
        } else {
            //Toast.makeText(this, getString(R.string.sui_starting), Toast.LENGTH_SHORT).show()
            FreeFormUtils.init(object : SuiServerListener() {
                override fun onStart() {
                    //需要--activity-clear-task防止前台被打开
                    FreeFormUtils.getControlService()?.startActivity("am start -n com.sunshine.freeform/.activity.floating_view.FloatingViewActivity --activity-clear-task")
                }

                override fun onStop() {
                    FreeFormUtils.init(null)
                }

            })
        }

        collapseStatusBar()
    }

    @SuppressLint("WrongConstant")
    fun collapseStatusBar() {
        val service = getSystemService("statusbar") ?: return
        try {
            val clazz = Class.forName("android.app.StatusBarManager")
            var collapse: Method? = null
            collapse = clazz.getMethod("collapsePanels")
            collapse.isAccessible = true
            collapse.invoke(service)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation != orientation) {
            orientation = newConfig.orientation
            //freeFormAppsFloatingView?.removeFloatingViewWindows()
            //freeFormAppsFloatingView?.showFloatingViewWindow()
        }
    }
}