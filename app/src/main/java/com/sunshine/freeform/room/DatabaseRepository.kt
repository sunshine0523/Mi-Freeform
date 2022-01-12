package com.sunshine.freeform.room

import android.content.Context
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.MyDatabase.Companion.getDatabase
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

/**
 * @author sunshine
 * @date 2021/1/31
 */
class DatabaseRepository(context: Context) {

    private val freeFormAppsDao: FreeFormAppsDao
    private val notificationAppsDao: NotificationAppsDao

    fun insertFreeForm(packageName: String, userId: Int) {
        try {
            freeFormAppsDao.insert(packageName, userId)
        }catch (e: Exception) { }
    }

    fun deleteFreeForm(packageName: String, userId: Int) {
        freeFormAppsDao.delete(packageName, userId)
    }

    fun getAllFreeFormName(): LiveData<List<String>?> {
        return freeFormAppsDao.getAllName()
    }

    fun getAllFreeForm() : LiveData<List<FreeFormAppsEntity>?> {
        return freeFormAppsDao.getAll()
    }

    fun getAllFreeFormAppsByFlow(): Flow<List<FreeFormAppsEntity>?> {
        return freeFormAppsDao.getAllByFlow()
    }

    fun getCount(): Int {
        return freeFormAppsDao.getCount()
    }

    fun update(entity: FreeFormAppsEntity) {
        freeFormAppsDao.update(entity)
    }

    fun getAllFreeFormWithoutLiveData() : List<String>? {
        return freeFormAppsDao.getAllWithoutLiveData()
    }

    fun deleteAllFreeForm() {
        freeFormAppsDao.deleteAll()
    }

    fun deleteMore(freeFormAppsEntityList: List<FreeFormAppsEntity>) {
        freeFormAppsDao.deleteList(freeFormAppsEntityList)
    }

    fun insertNotification(packageName: String, userId: Int) {
        try {
            notificationAppsDao.insert(packageName, userId)
        }catch (e: Exception) {}

    }

    fun deleteNotification(packageName: String, userId: Int) {
        notificationAppsDao.delete(packageName, userId)
    }

    fun getAllNotification() : LiveData<List<NotificationAppsEntity>?> {
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