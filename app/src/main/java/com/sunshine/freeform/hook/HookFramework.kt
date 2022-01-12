package com.sunshine.freeform.hook

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.os.Handler
import android.view.*
import com.sunshine.freeform.hook.service.MiFreeFormService
import com.sunshine.freeform.hook.utils.InputEventHookUtils
import com.sunshine.freeform.hook.utils.ShellHookUtils
import com.sunshine.freeform.hook.view.FreeFormHookWindow
import de.robv.android.xposed.*
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import java.lang.reflect.Constructor


class HookFramework : IXposedHookZygoteInit {

    @SuppressLint("PrivateApi")
    override fun initZygote(startupParam: StartupParam?) {
        val activityThread =
            Class.forName("android.app.ActivityThread")
        XposedBridge.hookAllMethods(activityThread, "systemMain", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                addMiFreeFormService()
                hookDMS()
            }
        })

    }

    private fun addMiFreeFormService() {
        val classLoader = Thread.currentThread().contextClassLoader
        val ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader)
        XposedBridge.hookAllConstructors(
            ams,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val service =
                        MiFreeFormService()
                    val sm = XposedHelpers.findClass("android.os.ServiceManager", classLoader)
                    XposedHelpers.callStaticMethod(
                        sm,
                        "addService",
                        "user.mifreeform",
                        service,
                        true
                    )
                    XposedBridge.log("Mi-freeform service has added.")
                }
            }
        )
    }

    private fun hookDMS() {
        val classLoader = Thread.currentThread().contextClassLoader
        val dms = XposedHelpers.findClass("com.android.server.display.DisplayManagerService", classLoader)
        XposedHelpers.findAndHookConstructor(
            dms,
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val dmsObj = param!!.thisObject

                    val mSyncRoot = XposedHelpers.getObjectField(dmsObj, "mSyncRoot")
                    val mContext = XposedHelpers.getObjectField(dmsObj, "mContext")
                    val mHandler = XposedHelpers.getObjectField(dmsObj, "mHandler")
                    val mDisplayAdapterListener = XposedHelpers.getObjectField(dmsObj, "mDisplayAdapterListener")
                    val mUiHandler = XposedHelpers.getObjectField(dmsObj, "mUiHandler")

                    val overlayDisplayAdapter = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter", classLoader)

                    val overlayDisplayAdapterConstructor = overlayDisplayAdapter.constructors[0]
                    overlayDisplayAdapterConstructor.isAccessible = true
                    val overlayDisplayAdapterObj = overlayDisplayAdapterConstructor.newInstance(
                        mSyncRoot,
                        mContext,
                        mHandler,
                        mDisplayAdapterListener,
                        mUiHandler
                    )
                    hookOverlay(classLoader, overlayDisplayAdapterObj, mHandler as Handler, mUiHandler as Handler, mContext as Context)
                }
            }
        )
    }

    private fun hookOverlay(
        classLoader: ClassLoader?,
        overlayDisplayAdapterObj: Any,
        dmsHandler: Handler,
        dmsUiHandler: Handler,
        dmsContext: Context
    ) {

        val overlayDisplayWindow = XposedHelpers.findClass("com.android.server.display.OverlayDisplayWindow", classLoader)

        //阻止自带的叠加层显示，但是不显示就不会调用Handle的onWindowCreated，从而不会创建Device，所以，我们手动创建
        XposedBridge.hookAllMethods(
            overlayDisplayWindow,
            "show",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    return null
                }
            }
        )

        hookGetDmsUiHandler(dmsUiHandler)
        hookGetDmsContext(dmsContext)
        hookAddOverlayDevice(classLoader, overlayDisplayAdapterObj)
        hookInjectEvent(dmsUiHandler)
        hookKillApp(dmsUiHandler)
        hookDestroy(overlayDisplayAdapterObj)
    }

    private fun hookGetDmsUiHandler(dmsUiHandler: Handler) {
        XposedHelpers.findAndHookMethod(
            FreeFormHookWindow::class.java,
            "getDmsUiHandler",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    param!!.result = dmsUiHandler
                }
            }
        )
    }

    private fun hookGetDmsContext(dmsContext: Context) {
        XposedHelpers.findAndHookMethod(
            FreeFormHookWindow::class.java,
            "getDmsContext",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    param!!.result = dmsContext
                }
            }
        )
    }

    private var overDisplayHandleObj: Any? = null

    private fun hookAddOverlayDevice(
        classLoader: ClassLoader?,
        overlayDisplayAdapterObj: Any
    ) {
        val overlayDisplayHandle = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayDisplayHandle", classLoader)
        val overlayMode = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayMode", classLoader)

        XposedHelpers.findAndHookMethod(
            FreeFormHookWindow::class.java,
            "addOverlayDevice",
            SurfaceTexture::class.java,
            Int::class.javaPrimitiveType,
            String::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    //这里要用隐藏构造函数，因为不是public
                    val constructor = overlayMode.declaredConstructors[0]
                    constructor.isAccessible = true
                    val overlayModeObj = constructor.newInstance(
                        param!!.args[3],
                        param.args[4],
                        param.args[5]
                    )

                    val modes = ArrayList<Any>()
                    modes.add(overlayModeObj)

                    val handleConstructor = overlayDisplayHandle.declaredConstructors[0]
                    handleConstructor.isAccessible = true

                    overDisplayHandleObj = when (android.os.Build.VERSION.SDK_INT) {
                        29 -> hookOverlayHandleQ(handleConstructor, overlayDisplayAdapterObj, param.args[2], modes, param.args[1])
                        30 -> hookOverlayHandleR(handleConstructor, overlayDisplayAdapterObj, param.args[2], modes, param.args[1], classLoader)
                        else -> null
                    }

                    XposedHelpers.callMethod(
                        overDisplayHandleObj,
                        "onWindowCreated",
                        param.args[0],
                        120.0f,
                        16666666L,
                        2
                    )
                }
            }
        )
    }

    private fun hookOverlayHandleQ(
        handleConstructor: Constructor<*>,
        overlayDisplayAdapterObj: Any,
        name: Any,
        modes: ArrayList<Any>,
        number: Any
    ): Any? {
        return handleConstructor.newInstance(
            overlayDisplayAdapterObj,
            name,    //name
            modes,
            51,
            false,
            number   //number need to unique
        )
    }

    private fun hookOverlayHandleR(
        handleConstructor: Constructor<*>,
        overlayDisplayAdapterObj: Any,
        name: Any,
        modes: ArrayList<Any>,
        number: Any,
        classLoader: ClassLoader?
    ): Any? {
        val overlayFlags = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayFlags", classLoader)
        val flagsConstructor = overlayFlags.declaredConstructors[0]
        flagsConstructor.isAccessible = true
        val flagsObj = flagsConstructor.newInstance(false, false, false)
        return handleConstructor.newInstance(
            overlayDisplayAdapterObj,
            name,    //name
            modes,
            51,
            flagsObj,
            number   //number need to unique
        )
    }

    private fun hookInjectEvent(dmsUiHandler: Handler) {
        XposedHelpers.findAndHookMethod(
            FreeFormHookWindow::class.java,
            "injectEvent",
            InputEvent::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    dmsUiHandler.post {
                        InputEventHookUtils.xposedInjectInputEvent(param!!.args[0] as InputEvent, param.args[1] as Int)
                    }
                }
            }
        )
    }

    private fun hookKillApp(dmsUiHandler: Handler) {
        XposedHelpers.findAndHookMethod(
            FreeFormHookWindow::class.java,
            "killApp",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    dmsUiHandler.post {
                        ShellHookUtils.execCommand("am force-stop ${param!!.args[0]}", false)
                    }
                }
            }
        )
    }

    private fun hookDestroy(
        overlayDisplayAdapterObj: Any
    ) {
        XposedHelpers.findAndHookMethod(
            FreeFormHookWindow::class.java,
            "destroy",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (null != overDisplayHandleObj) {
                        XposedHelpers.callMethod(
                            overDisplayHandleObj,
                            "onWindowDestroyed"
                        )

                        (XposedHelpers.getObjectField(overlayDisplayAdapterObj, "mOverlays") as ArrayList<*>).remove(overDisplayHandleObj)
                        overDisplayHandleObj = null
                        //FreeFormHookWindow
                        param!!.thisObject = null
                    }
                }
            }
        )
    }
}