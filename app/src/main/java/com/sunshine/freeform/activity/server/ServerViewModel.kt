package com.sunshine.freeform.activity.server

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.sunshine.freeform.utils.ShellUtils

/**
 * @author sunshine
 * @date 2021/2/1
 */
class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val sp =
        application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

    /**
     * 远程服务是否开启
     */
    fun serviceIsClose(): Boolean {
        val pid = ShellUtils.execCommand(
            "ps -ef | grep com.sunshine.freeform.Server | grep -v grep | awk '{print \$2}'",
            true
        ).successMsg
        return pid.isNullOrBlank()
    }

    fun getServerVersion(): Long {
        return sp.getLong("server_version", -1)
    }

    fun putServerVersion(version: Long) {
        sp.edit().putLong("server_version", version).apply()
    }
}