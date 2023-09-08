package com.sunshine.freeform

import android.app.Application
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
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }
}