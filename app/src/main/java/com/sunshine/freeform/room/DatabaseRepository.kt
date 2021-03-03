package com.sunshine.freeform.room

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.MyDatabase.Companion.getDatabase
import java.lang.Exception

/**
 * @author sunshine
 * @date 2021/1/31
 */
class DatabaseRepository(context: Context) {

    private val freeFormAppsDao: FreeFormAppsDao
    private val notificationAppsDao: NotificationAppsDao

    fun insertFreeForm(packageName: String) {
        try {
            freeFormAppsDao.insert(packageName)
        }catch (e: Exception) { }
    }

    fun deleteFreeForm(packageName: String) {
        freeFormAppsDao.delete(packageName)
    }

    fun getAllFreeForm() : LiveData<List<String>?> {
        return freeFormAppsDao.getAll()
    }

    fun getAllFreeFormWithoutLiveData() : List<String>? {
        return freeFormAppsDao.getAllWithoutLiveData()
    }

    fun deleteAllFreeForm() {
        freeFormAppsDao.deleteAll()
    }

    fun insertNotification(packageName: String) {
        try {
            notificationAppsDao.insert(packageName)
        }catch (e: Exception) {}

    }

    fun deleteNotification(packageName: String) {
        notificationAppsDao.delete(packageName)
    }

    fun getAllNotification() : LiveData<List<String>?> {
        return notificationAppsDao.getAll()
    }

    fun deleteAllNotification() {
        notificationAppsDao.deleteAll()
    }

    init {
        val database = getDatabase(context)
        freeFormAppsDao = database.freeFormAppsDao
        notificationAppsDao = database.notificationAppsDao
    }
}