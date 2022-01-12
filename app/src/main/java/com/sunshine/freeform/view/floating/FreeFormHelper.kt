package com.sunshine.freeform.view.floating

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.sunshine.freeform.BuildConfig
import com.sunshine.freeform.IControlService
import com.sunshine.freeform.activity.floating_view.FreeFormWindowAbs
import com.sunshine.freeform.callback.FreeFormListener
import com.sunshine.freeform.callback.SuiServerListener
import com.sunshine.freeform.service.ControlService
import com.sunshine.freeform.utils.FreeFormUtils
import com.sunshine.freeform.utils.MyLog
import com.sunshine.freeform.utils.StackSet
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.util.*
import kotlin.collections.HashSet

/**
 * @author sunshine
 * @date 2022/1/6
 */
object FreeFormHelper {
    private const val TAG = "FreeFormHelper"

    //shizuku service.
    //And judge shizuku&sui service is running.
    //If not, when new context band this class
    //Try to run service.
    private var controlService: IControlService? = null

    //IF THIS LISTENER IS NULL, THIS CLASS CAN NOT RUN!
    //BECAUSE SOME METHOD NEED CONTEXT
    private var suiServerListener: SuiServerListener? = null

    //小窗的顺序
    var displayIdStackSet = StackSet<Int>()

    //所有小窗的集合，用于屏幕旋转时监听
    var freeFormViewSet = HashSet<FreeFormView>()

    fun init(context: Context, suiServerListener: SuiServerListener) {
        this.suiServerListener = suiServerListener

        if (!Shizuku.pingBinder() || controlService == null) {
            bindRemoteService()
        } else {
            suiServerListener.onStart()
        }
    }

    //Bind shizuku&sui remote service
    private fun bindRemoteService() {
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
                suiServerListener?.onStop()
                //防止再次监听
                suiServerListener = null
                controlService = null
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                controlService = IControlService.Stub.asInterface(service)
                if (controlService!!.init()) {
                    suiServerListener?.onStart()
                    suiServerListener = null
                } else {
                    suiServerListener?.onFailBind()
                    suiServerListener = null
                    controlService = null
                }
            }
        }

        try {
            Shizuku.bindUserService(
                userServiceArgs,
                userServiceConnection
            )
        }catch (e: Exception) {
            suiServerListener?.onFailBind()
            suiServerListener = null
        }
    }

    fun getControlService(): IControlService? {
        return controlService
    }

    /**
     * 显示的小窗中是否存在要打开的小窗，存在就不允许打开了
     */
    fun hasFreeFormWindow(command: String): Boolean {
        freeFormViewSet.forEach {
            if (command == it.command) return true
        }
        return false
    }

    fun onOrientationChanged(){
        freeFormViewSet.forEach {
            it.onOrientationChanged()
        }
    }
}