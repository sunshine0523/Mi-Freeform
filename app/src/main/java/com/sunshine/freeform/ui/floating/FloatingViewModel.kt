package com.sunshine.freeform.ui.floating

import android.content.Context
import com.sunshine.freeform.MiFreeform
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.FreeFormAppsEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.min

/**
 * @author sunshine
 * @date 2022/1/6
 */
class FloatingViewModel(context: Context) {
    private val repository = DatabaseRepository(context)
    private val sp = context.applicationContext.getSharedPreferences(MiFreeform.CONFIG, Context.MODE_PRIVATE)
    val screenWidth = min(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
    val screenHeight = max(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
    val screenDensityDpi = context.resources.displayMetrics.densityDpi

    fun getAllFreeFormApps(): Flow<List<FreeFormAppsEntity>?> {
        return repository.getAllFreeFormAppsByFlow()
    }

    fun deleteNotInstall(notInstallList: List<FreeFormAppsEntity>) {
        repository.deleteMore(notInstallList)
    }

    fun getIntSp(name: String, defaultValue: Int): Int {
        if (sp.contains(name).not()) sp.edit().putInt(name, defaultValue).apply()
        return sp.getInt(name, defaultValue)
    }
}