package com.sunshine.freeform.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * @author sunshine
 * @date 2021/1/31
 */
@Dao
interface FreeFormAppsDao {

    @Query("INSERT INTO FreeFormAppsEntity(packageName) VALUES(:packageName)")
    fun insert(packageName: String)

    @Query("DELETE FROM FreeFormAppsEntity WHERE packageName = :packageName")
    fun delete(packageName: String)

    @Query("SELECT packageName FROM FreeFormAppsEntity")
    fun getAll() : LiveData<List<String>?>
}