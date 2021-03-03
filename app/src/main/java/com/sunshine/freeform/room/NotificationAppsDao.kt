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
interface NotificationAppsDao {

    @Query("INSERT INTO NotificationAppsEntity(packageName) VALUES(:packageName)")
    fun insert(packageName: String)

    @Query("DELETE FROM NotificationAppsEntity WHERE packageName = :packageName")
    fun delete(packageName: String)

    @Query("SELECT packageName FROM NotificationAppsEntity")
    fun getAll() : LiveData<List<String>?>

    @Query("DELETE FROM NotificationAppsEntity")
    fun deleteAll()
}