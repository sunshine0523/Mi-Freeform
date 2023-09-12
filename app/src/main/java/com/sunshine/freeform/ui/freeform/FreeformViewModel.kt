package com.sunshine.freeform.ui.freeform

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.sunshine.freeform.MiFreeform
import kotlin.math.max
import kotlin.math.min

/**
 * @author KindBrave
 * @since 2023/9/11
 */
class FreeformViewModel(private val application: Application) : AndroidViewModel(application) {
    val screenWidth = min(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenHeight = max(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenDensityDpi = application.resources.displayMetrics.densityDpi
    private val sp = application.applicationContext.getSharedPreferences(MiFreeform.CONFIG, Context.MODE_PRIVATE)

    fun getIntSp(name: String, defaultValue: Int): Int {
        return sp.getInt(name, defaultValue)
    }
}