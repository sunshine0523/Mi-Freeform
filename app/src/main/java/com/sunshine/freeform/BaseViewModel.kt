package com.sunshine.freeform

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.sunshine.freeform.service.ControlService
import rikka.shizuku.Shizuku

/**
 * @author sunshine
 * @date 2022/1/28
 */
class BaseViewModel(application: Application) : AndroidViewModel(application) {

    val isRunning = MutableLiveData(false)
    private var controlService: IControlService? = null
    private var sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val onRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SUI_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                bindShizukuService()
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
            Shizuku.requestPermission(SUI_CODE)
        } else {
            bindShizukuService()
        }
    }

    companion object {
        private const val TAG = "BaseViewModel"
        private const val SUI_CODE = 0
        fun get(): BaseViewModel {
            return ViewModelProvider(MiFreeForm.me, ViewModelProvider.AndroidViewModelFactory(MiFreeForm.me)).get(BaseViewModel::class.java)
        }
    }

    fun initShizuku() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
    }

    private fun bindShizukuService() {
        val userServiceArgs =
            Shizuku.UserServiceArgs(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    ControlService::class.java.name
                )
            )
                .processNameSuffix("service")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)

        val userServiceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                isRunning.value = false
                controlService = null
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                controlService = IControlService.Stub.asInterface(service)
                if (controlService!!.init()) isRunning.value = true
            }
        }

        Shizuku.bindUserService(userServiceArgs, userServiceConnection)

    }

    fun getControlService() = controlService

    fun getIntFromSP(key: String, defaultValue: Int): Int {
        return sp.getInt(key, defaultValue)
    }
}