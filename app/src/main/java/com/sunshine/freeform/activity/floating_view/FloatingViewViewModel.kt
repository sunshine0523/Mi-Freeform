package com.sunshine.freeform.activity.floating_view

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
class FloatingViewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatabaseRepository(application)
    private val sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

    fun getAllApps(): LiveData<List<FreeFormAppsEntity>?> {
        return repository.getAllFreeForm()
    }

    fun getPosition(): Boolean {
        return sp.getBoolean("switch_show_location", false)
    }

    fun isShowFloating(): Boolean {
        return sp.getBoolean("switch_floating", false)
    }

    fun deleteNotInstall(notInstallList: List<FreeFormAppsEntity>) {
        repository.deleteMore(notInstallList)
    }

    fun isUseSystemFreeForm(): Boolean {
        return sp.getBoolean("switch_use_system_freeform", false)
    }
}