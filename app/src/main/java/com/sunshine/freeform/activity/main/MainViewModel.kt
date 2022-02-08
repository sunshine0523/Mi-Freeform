package com.sunshine.freeform.activity.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.NotificationAppsEntity

/**
 * @author sunshine
 * @date 2021/2/1
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
    private val repository = DatabaseRepository(application)

    fun isNotification(): Boolean {
        return sp.getBoolean("switch_notify", false)
    }

    fun getAllNotificationApps(): LiveData<List<NotificationAppsEntity>?> {
        return repository.getAllNotification()
    }
}