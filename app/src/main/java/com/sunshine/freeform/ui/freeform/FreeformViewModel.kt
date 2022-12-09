package com.sunshine.freeform.ui.freeform

import android.content.Context
import com.sunshine.freeform.app.MiFreeform

class FreeformViewModel(private val context: Context) {

    private val sp = context.getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)

    fun getBooleanSp(key: String, defaultValue: Boolean): Boolean {
        return sp.getBoolean(key, defaultValue)
    }

    fun getIntSp(key: String, defaultValue: Int): Int {
        return sp.getInt(key, defaultValue)
    }
}