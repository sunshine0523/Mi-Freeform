package com.sunshine.freeform.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * @author sunshine
 * @date 2021/1/31
 */
@Dao
interface FreeFormAppsDao {

    @Query("INSERT INTO FreeFormAppsEntity(packageName, userId) VALUES(:packageName, :userId)")
    fun insert(packageName: String, userId: Int)

    @Query("DELETE FROM FreeFormAppsEntity WHERE packageName = :packageName and userId = :userId")
    fun delete(packageName: String, userId: Int)

    @Query("SELECT * FROM FreeFormAppsEntity")
    fun getAll() : LiveData<List<FreeFormAppsEntity>?>

    @Query("SELECT * FROM FreeFormAppsEntity")
    fun getAllByFlow() : Flow<List<FreeFormAppsEntity>?>

    @Query("SELECT packageName FROM FreeFormAppsEntity")
    fun getAllName() : LiveData<List<String>?>

    @Query("SELECT packageName FROM FreeFormAppsEntity")
    fun getAllWithoutLiveData() : List<String>?

    @Query("SELECT COUNT(*) FROM FreeFormAppsEntity")
    fun getCount(): Int

    @Query("DELETE FROM FreeFormAppsEntity")
    fun deleteAll()

    @Delete
    fun deleteList(freeFormAppsEntityList: List<FreeFormAppsEntity>)

    @Update
    fun update(entity: FreeFormAppsEntity)
}