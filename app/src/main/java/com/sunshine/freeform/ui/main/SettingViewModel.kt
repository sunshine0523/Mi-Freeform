package com.sunshine.freeform.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.sunshine.freeform.MiFreeform
import com.sunshine.freeform.MiFreeformServiceManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/8/26
 */
class SettingViewModel(private val application: Application) : AndroidViewModel(application) {
    private val sp = application.applicationContext.getSharedPreferences(MiFreeform.CONFIG, Context.MODE_PRIVATE)

    val screenWidth = min(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenHeight = max(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenDensityDpi = application.resources.displayMetrics.densityDpi

    private val _enableSideBar = MutableLiveData<Boolean>()
    val enableSideBar: LiveData<Boolean> get() = _enableSideBar
    private val _freeformWidth = MutableLiveData<Int>()
    val freeformWidth: LiveData<Int> get() = _freeformWidth
    private val _freeformHeight = MutableLiveData<Int>()
    val freeformHeight: LiveData<Int> get() = _freeformHeight
    private val _freeformDensityDpi = MutableLiveData<Int>()
    val freeformDensityDpi: LiveData<Int> get() = _freeformDensityDpi

    init {
        val remoteSetting = Gson().fromJson(MiFreeformServiceManager.getSetting(), RemoteSettings::class.java) ?: RemoteSettings()
        _enableSideBar.postValue(remoteSetting.enableSideBar)
        _freeformWidth.postValue(sp.getInt("freeform_width", (screenWidth * 0.8).roundToInt()))
        _freeformHeight.postValue(sp.getInt("freeform_height", (screenHeight * 0.5).roundToInt()))
        _freeformDensityDpi.postValue(sp.getInt("freeform_dpi", screenDensityDpi))
    }

    fun saveRemoteSidebar(enableSideBar: Boolean) {
        _enableSideBar.postValue(enableSideBar)
        if (enableSideBar) {
            MiFreeformServiceManager
        }
    }

    fun setFreeformWidth(width: Int) {
        setIntSp(width, "freeform_width")
        _freeformWidth.postValue(width)
    }

    fun setFreeformHeight(height: Int) {
        setIntSp(height, "freeform_height")
        _freeformHeight.postValue(height)
    }

    fun setFreeformDpi(dpi: Int) {
        setIntSp(dpi, "freeform_dpi")
        _freeformDensityDpi.postValue(dpi)
    }

    private fun setIntSp(value: Int, name: String) {
        sp.edit().apply {
            putInt(name, value)
            apply()
        }
    }
}