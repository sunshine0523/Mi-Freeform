package com.sunshine.freeform.activity.choose_free_form_apps

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository

/**
 * @author sunshine
 * @date 2021/1/31
 */
class ChooseFreeFormAppsViewModel(application: Application) : AndroidViewModel(application){

    private val repository = DatabaseRepository(application)

    fun getAllFreeFormApps(): LiveData<List<String>?> {
        return repository.getAll()
    }

    fun insertApps(packageName: String) {
        repository.insert(packageName)
    }

    fun deleteApps(packageName: String) {
        repository.delete(packageName)
    }
}