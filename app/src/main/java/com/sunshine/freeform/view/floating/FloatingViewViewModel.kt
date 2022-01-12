package com.sunshine.freeform.view.floating

import android.content.Context
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.FreeFormAppsEntity
import kotlinx.coroutines.flow.Flow

/**
 * @author sunshine
 * @date 2022/1/6
 */
class FloatingViewViewModel(context: Context) {
    private val repository = DatabaseRepository(context)

    fun getAllFreeFormApps(): Flow<List<FreeFormAppsEntity>?> {
        return repository.getAllFreeFormAppsByFlow()
    }

    fun deleteNotInstall(notInstallList: List<FreeFormAppsEntity>) {
        repository.deleteMore(notInstallList)
    }

}