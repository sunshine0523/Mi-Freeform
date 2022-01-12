package com.sunshine.freeform

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import rikka.sui.Sui

/**
 * @author sunshine
 * @date 2021/3/17
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            Sui.init(BuildConfig.APPLICATION_ID)
        } catch (e: Exception) {}
    }
}

val Context.dataStore by preferencesDataStore(name  = "app_settings")