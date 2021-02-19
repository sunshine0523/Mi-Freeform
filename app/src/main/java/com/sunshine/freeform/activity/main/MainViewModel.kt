package com.sunshine.freeform.activity.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.utils.ShellUtils

/**
 * @author sunshine
 * @date 2021/2/1
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
    private val repository = DatabaseRepository(application)

    fun isShowFloating(): Boolean {
        return sp.getBoolean("switch_floating", false)
    }

    fun closeService() {
        //关闭服务进程
        val pid = ShellUtils.execCommand("ps -ef | grep com.sunshine.freeform.Server | grep -v grep | awk '{print \$2}'", true).successMsg
        ShellUtils.execRootCmdSilent("kill -9 $pid")
        sp.edit().apply {
            putBoolean("switch_service", false)
            putBoolean("switch_floating", false)
            apply()
        }
    }

    fun getAllFreeFormApps(): LiveData<List<String>?> {
        return repository.getAll()
    }
}