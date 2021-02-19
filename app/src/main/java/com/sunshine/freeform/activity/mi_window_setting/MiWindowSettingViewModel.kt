package com.sunshine.freeform.activity.mi_window_setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sunshine.freeform.room.DatabaseRepository

/**
 * @author sunshine
 * @date 2021/1/31
 */
class MiWindowSettingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatabaseRepository(application)

    fun getAllFreeFormApps(): LiveData<List<String>?> {
        return repository.getAll()
    }
}