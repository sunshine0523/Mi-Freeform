package com.sunshine.freeform.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class FloatingService : Service() {

    private lateinit var viewModel: ServiceViewModel
    private var isShowingSidebar = false

    override fun onBind(intent: Intent): IBinder {
        viewModel = ServiceViewModel(this.application)
        return MyBinder()
    }

    fun getViewModel(): ServiceViewModel {
        return viewModel
    }

    fun getShowingSidebar(): Boolean {
        return isShowingSidebar
    }

    fun setShowingSidebar(isShowingSidebar: Boolean) {
        this.isShowingSidebar = isShowingSidebar
    }

    inner class MyBinder : Binder() {
        fun getService(): FloatingService {
            return this@FloatingService
        }
    }
}