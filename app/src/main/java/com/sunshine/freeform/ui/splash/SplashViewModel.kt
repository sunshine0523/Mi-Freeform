package com.sunshine.freeform.ui.splash

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.sunshine.freeform.app.MiFreeform

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val sp = application.getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)

    fun getBooleanSp(key: String, default: Boolean): Boolean {
        return sp.getBoolean(key, default)
    }

    fun getIntSp(key: String, default: Int): Int {
        return sp.getInt(key, default)
    }

    fun putIntSp(key: String, value: Int) {
        sp.edit().putInt(key, value).apply()
    }
}