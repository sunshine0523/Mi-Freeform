package com.sunshine.freeform.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

/**
 * @author sunshine
 * @date 2021/1/31
 */
@Dao
interface NotificationAppsDao {

    @Query("INSERT INTO NotificationAppsEntity(packageName, userId) VALUES(:packageName, :userId)")
    fun insert(packageName: String, userId: Int)

    @Query("DELETE FROM NotificationAppsEntity WHERE packageName = :packageName and userId = :userId")
    fun delete(packageName: String, userId: Int)

    @Query("SELECT * FROM NotificationAppsEntity")
    fun getAll() : LiveData<List<NotificationAppsEntity>?>

    @Query("DELETE FROM NotificationAppsEntity")
    fun deleteAll()
}