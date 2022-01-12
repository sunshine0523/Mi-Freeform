package com.sunshine.freeform.utils

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.sunshine.freeform.BuildConfig
import com.sunshine.freeform.IControlService
import com.sunshine.freeform.activity.floating_view.FreeFormWindowAbs
import com.sunshine.freeform.callback.IOnRotationChangedListener
import com.sunshine.freeform.callback.SuiServerListener
import com.sunshine.freeform.service.ControlService
import com.sunshine.freeform.service.FloatingService
import rikka.shizuku.Shizuku
import kotlin.collections.HashSet

/**
 * @author sunshine
 * @date 2021/2/19
 * 一些小窗配置
 */
object FreeFormUtils {

    private const val TAG = "FreeFormConfig"

    var dpi = 300   //小窗默认分辨率为300dpi，可以自定义

    //所有小窗的集合，用于屏幕旋转时监听
    var freeFormViewSet = HashSet<FreeFormWindowAbs>()

    //屏幕方向，1 竖屏 2横屏 0未定义
    var orientation = Configuration.ORIENTATION_UNDEFINED

    //小窗的顺序
    var displayIdStackSet = StackSet<Int>()

    //存放最小化小窗的列表
    var smallFreeFormList = ArrayList<FreeFormWindowAbs>()

    //最小化的边缘
    const val SMALL_FREEFORM_POSITION = 250

    fun startActivityForHook(packageName: String) {
        var freeFormWindow: FreeFormWindowAbs? = null
        freeFormViewSet.forEach {
            if (it.packageName == packageName) {
                freeFormWindow = it
                return@forEach
            }
        }
        if (freeFormWindow != null) {
            ShellUtils.execCommand("${freeFormWindow!!.command}${freeFormWindow!!.displayId}", true)
        }
    }

    private var controlServiceInitSuccess = false

    private var controlService: IControlService? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    //sui服务状态监听
    private var listener: SuiServerListener? = null

    private val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                ControlService::class.java.name
            )
        )
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            listener?.onStop()

            controlService = null
            controlServiceInitSuccess = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            controlService = IControlService.Stub.asInterface(service)
            //只有初始化成功了，才能设置为true
            try {
                if (controlService!!.init()) {
                    //给调用者回调
                    listener?.onStart()

                    controlServiceInitSuccess = true

//                    //获取当前屏幕方向
//                    val rotation = controlService!!.rotation
//                    when {
//                        rotation < 0 -> orientation = Configuration.ORIENTATION_UNDEFINED
//                        rotation % 2 == 0 -> Configuration.ORIENTATION_PORTRAIT
//                        else -> Configuration.ORIENTATION_LANDSCAPE
//                    }

//                    //初始化屏幕方向监听
//                    controlService!!.initRotationWatcher(object : IOnRotationChangedListener.Stub() {
//                        override fun onRotationChanged(rotation: Int) {
//                            mainHandler.post {
//                                orientation = if (rotation % 2 == 0) Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
//                                orientationChanged()
//                            }
//                        }
//                    })
                }
            } catch (e: Exception) {
                MyLog.e(TAG, "onServiceConnected", e.toString())
            }
        }
    }

    fun init(listener: SuiServerListener?) {
        try {
            this.listener = listener
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            Shizuku.bindUserService(
                userServiceArgs,
                userServiceConnection
            )
        } catch (e: Exception) {
            controlServiceInitSuccess = false
            MyLog.e(TAG, "init", e.toString())
        }
    }

    fun getControlService(): IControlService? {
        return controlService
    }

    fun serviceInitSuccess(): Boolean {
        return controlServiceInitSuccess
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

    /**
     * 通知悬浮按钮更变位置
     * 屏幕旋转时，需要获取所有显示的小窗
     * 然后移除掉并重新创建
     * 屏幕方向通过无障碍直接设置，不需要传送参数，这样可以在第一次就知道屏幕的方向
     */
    fun orientationChanged() {
        FloatingService.orientationChangedListener?.onChanged(orientation)
        freeFormViewSet.forEach {
            it.resize()
        }
    }

    /**
     * 当销毁服务时，因为这个是全局的，所以不会销毁，只有重启软件才会销毁
     * 而这样如果不重启软件而只是重启服务，这里就会有问题
     * 在服务的销毁中调用
     */
    fun onDelete(removeAllFreeForm: Boolean) {
        if (removeAllFreeForm) {
            freeFormViewSet.forEach {
                it.destroy()
            }
        }
    }
}