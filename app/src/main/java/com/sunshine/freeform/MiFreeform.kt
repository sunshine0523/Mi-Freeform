package com.sunshine.freeform

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MiFreeform: Application() {

    override fun onCreate() {
        super.onCreate()
        MiFreeformServiceManager.init()
    }

    companion object {
        private const val TAG = "Mi-Freeform"
        const val CONFIG = "config"

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.addHiddenApiExemptions("")
            }
        }
    }
}