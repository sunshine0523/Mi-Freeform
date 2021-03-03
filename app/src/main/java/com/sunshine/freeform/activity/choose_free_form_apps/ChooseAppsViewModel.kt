package com.sunshine.freeform.activity.choose_free_form_apps

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository

/**
 * @author sunshine
 * @date 2021/1/31
 */
class ChooseAppsViewModel(application: Application) : AndroidViewModel(application){

    private val repository = DatabaseRepository(application)

    var type = 1

    fun getAllApps(): LiveData<List<String>?> {
        return if (type == 1) repository.getAllFreeForm() else repository.getAllNotification()
    }

    fun insertApps(packageName: String) {
        if (type == 1) repository.insertFreeForm(packageName) else repository.insertNotification(packageName)

    }

    fun deleteApps(packageName: String) {
        if (type == 1) repository.deleteFreeForm(packageName) else repository.deleteNotification(packageName)
    }

    fun deleteAll() {
        if (type == 1) repository.deleteAllFreeForm() else repository.deleteAllNotification()
    }

    //添加列表中所有软件
    fun insertAllApps(packages: MutableList<ResolveInfo>?) {
        deleteAll()
        packages?.forEach {
            insertApps(it.activityInfo.applicationInfo.packageName)
        }
    }

}