package com.sunshine.freeform.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.*
import com.google.android.material.color.DynamicColors
import com.sunshine.freeform.BuildConfig
import com.sunshine.freeform.IControlService
import com.sunshine.freeform.service.ControlService
import com.tencent.bugly.crashreport.CrashReport
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.lang.StringBuilder

/**
 * @author sunshine
 * @date 2021/3/17
 */
class MiFreeform : Application() {
    val isRunning = MutableLiveData(false)
    private var controlService: IControlService? = null

    private val onRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SUI_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                initShizuku()
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SUI_CODE)
        } else {
            initShizuku()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isRunning.postValue(false)
    }

    private val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                ControlService::class.java.name
            )
        ).processNameSuffix("service")

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isRunning.value = false
            controlService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            controlService = IControlService.Stub.asInterface(service)
            if (controlService!!.init()) isRunning.value = true
        }
    }

    companion object {
        var me: MiFreeform? = null
        private const val TAG = "MiFreeForm"
        const val PACKAGE_NAME = "com.sunshine.freeform"
        //软件版本，该版本指需要再次展示介绍界面的版本
        const val VERSION = 1
        //隐私策略版本，用于展示隐私策略
        const val VERSION_PRIVACY = 1
        const val APP_SETTINGS_NAME = "app_settings"
        private const val SUI_CODE = 0

        private val log = StringBuilder()

        fun addLog(tag: String, functionName: String,  e: Exception) {
            log.append("$tag,$functionName:${e.message}")
        }

        init {
            Sui.init(BuildConfig.APPLICATION_ID)
        }
    }

    override fun onCreate() {
        super.onCreate()
        me = this

        DynamicColors.applyToActivitiesIfAvailable(this);

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        val sp = getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
        if (sp.getInt("version_privacy", -1) >= VERSION_PRIVACY) {
            CrashReport.initCrashReport(applicationContext)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

    private fun bindShizukuService() {
        try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (e: Exception) {
            addLog(TAG, "bindShizukuService", e)
        }
    }

    fun initShizuku() {
        if (controlService?.asBinder()?.pingBinder() == true) return
        bindShizukuService()
    }

    fun initShizuku(callback: ShizukuBindCallback) {
        initShizuku()
    }

    fun getControlService() = controlService

    interface ShizukuBindCallback {
        fun onBind()
    }
}