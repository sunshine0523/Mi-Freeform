package com.sunshine.freeform.room

import android.content.Context
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.MyDatabase.Companion.getDatabase
import java.lang.Exception

/**
 * @author sunshine
 * @date 2021/1/31
 */
class DatabaseRepository(context: Context) {

    private val freeFormAppsDao: FreeFormAppsDao

    fun insert(packageName: String) {
        try {
            freeFormAppsDao.insert(packageName)
        }catch (e: Exception) {}

    }

    fun delete(packageName: String) {
        freeFormAppsDao.delete(packageName)
    }

    fun getAll() : LiveData<List<String>?> {
        return freeFormAppsDao.getAll()
    }

    init {
        val database = getDatabase(context)
        freeFormAppsDao = database.freeFormAppsDao
    }
}