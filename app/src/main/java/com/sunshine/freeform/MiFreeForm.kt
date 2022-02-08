package com.sunshine.freeform

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import rikka.sui.Sui

/**
 * @author sunshine
 * @date 2021/3/17
 */
class MiFreeForm : Application(), ViewModelStoreOwner {

    companion object {
        lateinit var me: MiFreeForm
        lateinit var baseViewModel: BaseViewModel
        private const val TAG = "MiFreeForm"
    }

    override fun onCreate() {
        super.onCreate()
        Sui.init(BuildConfig.APPLICATION_ID)
        me = this
        baseViewModel = BaseViewModel.get()
    }

    override fun getViewModelStore(): ViewModelStore {
        return ViewModelStore()
    }
}

val Context.dataStore by preferencesDataStore(name  = "app_settings")