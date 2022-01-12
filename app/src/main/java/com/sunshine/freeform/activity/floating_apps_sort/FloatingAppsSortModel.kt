package com.sunshine.freeform.activity.floating_apps_sort

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.FreeFormAppsEntity

/**
 * @author sunshine
 * @date 2021/3/7
 */
class FloatingAppsSortModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatabaseRepository(application)

    fun getAllApps(): LiveData<List<FreeFormAppsEntity>?> {
        return repository.getAllFreeForm()
    }

    fun update(entity: FreeFormAppsEntity) {
        repository.update(entity)
    }

    fun deleteNotInstall(notInstallList: List<FreeFormAppsEntity>) {
        repository.deleteMore(notInstallList)
    }
}