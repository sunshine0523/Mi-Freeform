package com.sunshine.freeform.ui.main

import android.R
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContentProviderCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.sunshine.freeform.MiFreeform
import com.sunshine.freeform.MiFreeformServiceManager
import java.util.concurrent.CompletableFuture
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/8/26
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sp = application.applicationContext.getSharedPreferences(MiFreeform.CONFIG, Context.MODE_PRIVATE)

    val screenWidth = min(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenHeight = max(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenDensityDpi = application.resources.displayMetrics.densityDpi

    private val remoteSetting = Gson().fromJson(MiFreeformServiceManager.getSetting(), RemoteSettings::class.java) ?: RemoteSettings()
    private val _enableSideBar = MutableLiveData<Boolean>()
    val enableSideBar: LiveData<Boolean> get() = _enableSideBar
    private val _showImeInFreeform = MutableLiveData<Boolean>()
    val showImeInFreeform: LiveData<Boolean> get() = _showImeInFreeform
    private val _notification = MutableLiveData<Boolean>()
    val notification: LiveData<Boolean> get() = _notification
    private val _freeformWidth = MutableLiveData<Int>()
    val freeformWidth: LiveData<Int> get() = _freeformWidth
    private val _freeformHeight = MutableLiveData<Int>()
    val freeformHeight: LiveData<Int> get() = _freeformHeight
    private val _freeformDensityDpi = MutableLiveData<Int>()
    val freeformDensityDpi: LiveData<Int> get() = _freeformDensityDpi

    private val _log = MutableLiveData<String>()
    val log: LiveData<String> get() = _log
    private val _logSoftWrap = MutableLiveData<Boolean>()
    val logSoftWrap: LiveData<Boolean> get() = _logSoftWrap

    init {
        _enableSideBar.postValue(remoteSetting.enableSideBar)
        _showImeInFreeform.postValue(remoteSetting.showImeInFreeform)
        _notification.postValue(remoteSetting.notification)
        _freeformWidth.postValue(sp.getInt("freeform_width", (screenWidth * 0.8).roundToInt()))
        _freeformHeight.postValue(sp.getInt("freeform_height", (screenHeight * 0.5).roundToInt()))
        _freeformDensityDpi.postValue(sp.getInt("freeform_dpi", screenDensityDpi))
        _log.postValue(MiFreeformServiceManager.getLog())
        _logSoftWrap.postValue(false)
    }

    fun saveRemoteSidebar(enableSideBar: Boolean) {
        _enableSideBar.postValue(enableSideBar)
        remoteSetting.enableSideBar = enableSideBar
        MiFreeformServiceManager.setSetting(remoteSetting)
    }

    fun saveShowImeInFreeform(showImeInFreeform: Boolean) {
        _showImeInFreeform.postValue(showImeInFreeform)
        remoteSetting.showImeInFreeform = showImeInFreeform
        MiFreeformServiceManager.setSetting(remoteSetting)
    }

    fun saveNotification(notification: Boolean) {
        _notification.postValue(notification)
        remoteSetting.notification = notification
        MiFreeformServiceManager.setSetting(remoteSetting)
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

    fun clearLog() {
        _log.postValue("")
        MiFreeformServiceManager.clearLog()
    }

    fun setLogSoftWrap(softWrap: Boolean) {
        _logSoftWrap.postValue(softWrap)
    }

    private fun setIntSp(value: Int, name: String) {
        sp.edit().apply {
            putInt(name, value)
            apply()
        }
    }
}