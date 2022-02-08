package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.sunshine.freeform.MiFreeForm
import com.sunshine.freeform.R
import com.sunshine.freeform.view.floating.FloatingView
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.DataOutputStream
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/27
 * 通知栏快捷磁贴服务
 */
@DelicateCoroutinesApi
class QuickStartTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (Settings.canDrawOverlays(this)) {
            val showLocation = sp.getInt(CoreService.SHOW_LOCATION, CoreService.SHOW_LOCATION_DEFAULT)
            FloatingView(
                this@QuickStartTileService,
                showLocation
            )
        } else {
            try {
                Toast.makeText(this, getString(R.string.request_overlay_permission), Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(
                    intent
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.request_overlay_permission_fail), Toast.LENGTH_LONG).show()
            }
        }

        collapseStatusBar()
    }

    @SuppressLint("WrongConstant")
    private fun collapseStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MiFreeForm.baseViewModel.getControlService()?.execShell("cmd statusbar collapse")
        } else {
            val service = getSystemService("statusbar") ?: return
            try {
                val clazz = Class.forName("android.app.StatusBarManager")
                val collapse: Method? = clazz.getMethod("collapsePanels")
                collapse?.isAccessible = true
                collapse?.invoke(service)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}