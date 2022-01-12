package com.sunshine.freeform.activity.choose_free_form_apps

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.content.pm.ResolveInfo
import android.os.UserManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.room.NotificationAppsEntity
import com.sunshine.freeform.systemapi.UserHandle

/**
 * @author sunshine
 * @date 2021/1/31
 */
class ChooseAppsViewModel(application: Application) : AndroidViewModel(application){

    private val repository = DatabaseRepository(application)

    var type = 1

    fun getAllApps(): LiveData<List<FreeFormAppsEntity>?> {
        return repository.getAllFreeForm()
    }

    fun getAllNotificationApps(): LiveData<List<NotificationAppsEntity>?> {
        return repository.getAllNotification()
    }

    fun insertApps(packageName: String, userId: Int) {
        when (type) {
            2 -> repository.insertNotification(packageName, userId)
            else -> repository.insertFreeForm(packageName, userId)
        }
    }

    fun deleteApps(packageName: String, userId: Int) {
        when (type) {
            2 -> repository.deleteNotification(packageName, userId)
            else -> {
                repository.deleteFreeForm(packageName, userId)
            }
        }
    }

    fun deleteAll() {
        when (type) {
            2 -> repository.deleteAllNotification()
            1 -> {
                repository.deleteAllFreeForm()
            }
        }
    }

    //添加列表中所有软件
    fun insertAllApps(allAppsList: ArrayList<LauncherActivityInfo>, userManager: UserManager) {
        deleteAll()
        allAppsList.forEach {
            when (type) {
                2 -> repository.insertNotification(it.applicationInfo.packageName, UserHandle.getUserId(it.user, it.applicationInfo.uid))
                else -> repository.insertFreeForm(it.applicationInfo.packageName, UserHandle.getUserId(it.user, it.applicationInfo.uid))
            }
        }
    }

}