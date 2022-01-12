//package com.sunshine.freeform.utils
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.content.pm.ActivityInfo
//import android.graphics.SurfaceTexture
//import android.view.InputEvent
//import de.robv.android.xposed.*
//import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
//import java.util.ArrayList
//
//
///**
// * @author sunshine
// * @date 2021/2/21
// * Hook InputManagerService
// * 在PE dipper Android 11上测试，无法直接通过反射获取到IMS
// * 所以使用Xposed在zygote初始化时获取IMS
// * nativeInjectInputEvent是一个native方法
// * 对于普通进程，只能注入到自己的进程中，而root和shell用户可以注入其他应用中
// * 其中第三个参数是注入事件的UID，我们只需要更改为root的UID 0，就可以实现我们的软件进行跨进程控制了
// */
//class HookFramework : IXposedHookZygoteInit {
//
//    @SuppressLint("PrivateApi")
//    override fun initZygote(startupParam: StartupParam?) {
//        val activityThread =
//            Class.forName("android.app.ActivityThread")
//        XposedBridge.hookAllMethods(activityThread, "systemMain", object : XC_MethodHook() {
//            override fun afterHookedMethod(param: MethodHookParam) {
//                //hookAMS()
//                //hookIMS()
//                hookAddService()
//                hookDMS()
//                hookVirtualDisplayDevice()
//                //hookTest2()
//                //0android 11上才支持
//                if (android.os.Build.VERSION.SDK_INT > 29) hookPhoneWindowManager()
//
//                hookOverlayDisplayDevice()
//            }
//        })
//
//    }
//
//    private var overlayDisplayAdapterObj: Any? = null
//
//    /**
//     * 20210729 添加系统服务
//     */
//    private fun hookAddService() {
//        val classLoader = Thread.currentThread().contextClassLoader
//        val ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader);
//        XposedBridge.hookAllConstructors(
//            ams,
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    context = XposedHelpers.getObjectField(param!!.thisObject, "mContext") as Context
//                    val service = MiFreeFormService(context!!)
//                    val sm = XposedHelpers.findClass("android.os.ServiceManager", classLoader)
//                    XposedHelpers.callStaticMethod(
//                        sm,
//                        "addService",
//                        "user.mifreeform",
//                        service,
//                        true
//                    )
//                }
//            }
//        )
//    }
//
//    private fun hookOverlayDisplayDevice() {
//        val classLoader = Thread.currentThread().contextClassLoader
//        val clazz = XposedHelpers.findClass("com.android.server.display.VirtualDisplayAdapter", classLoader)
//        val clazz1 = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayDisplayHandle", classLoader)
//
//        XposedBridge.hookAllMethods(
//            clazz,
//            "createVirtualDisplayLocked",
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    XposedHelpers.callMethod(param!!.thisObject, "")
//                }
//            }
//        )
//
////        XposedHelpers.findAndHookMethod(
////            clazz1,
////            "onWindowCreated",
////            SurfaceTexture::class.java,
////            Float::class.javaPrimitiveType,
////            Long::class.javaPrimitiveType,
////            Int::class.javaPrimitiveType,
////            object : XC_MethodHook() {
////                override fun beforeHookedMethod(param: MethodHookParam?) {
////                    XposedBridge.log("${param!!.args[0]} ${param.args[1]} ${param.args[2]} ${param.args[3]}")
////                }
////            }
////        )
//
////        XposedBridge.hookAllConstructors(
////            clazz,
////            object : XC_MethodHook() {
////                override fun afterHookedMethod(param: MethodHookParam?) {
////                    XposedHelpers.callMethod(param!!.thisObject, "onWindowCreated", null, 60.0f, 0L, 0)
////
////
////                    XposedBridge.hookAllConstructors(
////                        clazz1,
////                        object : XC_MethodHook() {
////                            override fun afterHookedMethod(param: MethodHookParam?) {
////                                XposedBridge.log(param!!.thisObject.toString())
////                            }
////                        }
////                    )
////                }
////            }
////        )
//
////        val overlayFlagsClazz = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayFlags", classLoader)
////
////        XposedHelpers.findAndHookConstructor(
////            overlayFlagsClazz,
////            classLoader,
////            Boolean::class.javaPrimitiveType,
////            Boolean::class.javaPrimitiveType,
////            Boolean::class.javaPrimitiveType
////        )
//
//
//        /**
//         * public OverlayDisplayDevice(IBinder displayToken, String name,
//        238                  List<OverlayMode> modes, int activeMode, int defaultMode,
//        239                  float refreshRate, long presentationDeadlineNanos,
//        240                  boolean secure, int state,
//        241                  SurfaceTexture surfaceTexture, int number) {
//         */
////        XposedHelpers.findAndHookConstructor(
////            clazz,
////            classLoader,
////            object :
////        )
////
////        val con = clazz.constructors[0]
////
////        val display = SurfaceControl.createDisplay("mi-freeform", false)
////        val obj = con.newInstance(
////            display,
////            "name",
////            null,
////            0,
////            0,
////            60.0f,
////            0L,
////            false,
////            0,
////            null,
////            0
////        )
////        XposedBridge.log(obj.toString())
//    }
//
//    private fun hookTest2() {
//        val classLoader = Thread.currentThread().contextClassLoader
//        val activityRecord = XposedHelpers.findClass("com.android.server.wm.ActivityRecord", classLoader)
//
//        XposedBridge.hookAllConstructors(
//            activityRecord,
//            object : XC_MethodHook(){
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//
//                    if (param != null) {
//                        val intent = param.args[6] as Intent
//                        val activityInfo = param.args[8] as ActivityInfo
//
//                        val activityStack = try {
//                            XposedHelpers.callMethod(param.args[15], "getFocusedStack")
//                        }catch (e: Exception) {
//
//                        }
//                    }
//
//                    param!!.args.forEach {
//                        try{
//                            if (it != null) XposedBridge.log(it.toString())
//                        }catch (e: Exception) {}
//
//                    }
//                }
//            }
//        )
//    }
//
//    private var context: Context? = null
//
//    private fun hookAMS() {
//        val classLoader = Thread.currentThread().contextClassLoader
//        val ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader);
//        XposedBridge.hookAllConstructors(
//                ams,
//                object : XC_MethodHook() {
//                    override fun afterHookedMethod(param: MethodHookParam?) {
//                        context = XposedHelpers.getObjectField(param!!.thisObject, "mContext") as Context
//                    }
//                }
//        )
//    }
//
//    private fun hookDMS() {
//        val classLoader = Thread.currentThread().contextClassLoader
//        val dms = XposedHelpers.findClass("com.android.server.display.DisplayManagerService", classLoader)
//        XposedHelpers.findAndHookConstructor(
//                dms,
//                Context::class.java,
//                object : XC_MethodHook() {
//                    override fun afterHookedMethod(param: MethodHookParam?) {
//                        XposedBridge.log("dms constructor")
//
//                        val dmsObj = param!!.thisObject
//
//                        val mSyncRoot = XposedHelpers.getObjectField(dmsObj, "mSyncRoot")
//                        val mContext = XposedHelpers.getObjectField(dmsObj, "mContext")
//                        val mHandler = XposedHelpers.getObjectField(dmsObj, "mHandler")
//                        val mDisplayAdapterListener = XposedHelpers.getObjectField(dmsObj, "mDisplayAdapterListener")
//                        val mUiHandler = XposedHelpers.getObjectField(dmsObj, "mUiHandler")
//
//                        val overlayDisplayAdapter = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter", classLoader)
//
//                        val overlayDisplayAdapterConstructor = overlayDisplayAdapter.constructors[0]
//                        overlayDisplayAdapterConstructor.isAccessible = true
//                        val overlayDisplayAdapterObj = overlayDisplayAdapterConstructor.newInstance(
//                            mSyncRoot,
//                            mContext,
//                            mHandler,
//                            mDisplayAdapterListener,
//                            mUiHandler
//                        )
//
//                        XposedBridge.log("overlayDisplayAdapter: $overlayDisplayAdapterObj")
//                        overlayFun(classLoader, overlayDisplayAdapterObj)
//                    }
//                }
//        )
//    }
//
//    private fun overlayFun(classLoader: ClassLoader?, overlayDisplayAdapterObj: Any) {
//
//        val overlayDisplayWindow = XposedHelpers.findClass("com.android.server.display.OverlayDisplayWindow", classLoader)
//
//        XposedBridge.hookAllMethods(
//            overlayDisplayWindow,
//            "show",
//            object : XC_MethodReplacement() {
//                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
//                    //这里阻止自带的叠加层显示，但是不显示就不会调用Handle的onWindowCreated，从而不会创建Device，所以，我们手动创建
//                    return null
//                }
//            }
//        )
//
//        XposedHelpers.findAndHookMethod(
//            MiFreeFormService::class.java,
//            "sayHello",
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    val overlayDisplayHandle = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayDisplayHandle", classLoader)
//                    val overlayMode = XposedHelpers.findClass("com.android.server.display.OverlayDisplayAdapter\$OverlayMode", classLoader)
//
//                    //这里要用隐藏构造函数，因为不是public
//                    val constructor = overlayMode.declaredConstructors[0]
//                    //不设置这个怎么反射...
//                    constructor.isAccessible = true
//                    val overlayModeObj = constructor.newInstance(
//                        1080,
//                        1920,
//                        320
//                    )
//
//                    val modes = ArrayList<Any>()
//                    modes.add(overlayModeObj)
//
//                    val handleConstructor = overlayDisplayHandle.constructors[0]
//                    handleConstructor.isAccessible = true
//
//                    val overDisplayHandleObj = handleConstructor.newInstance(
//                        overlayDisplayAdapterObj,
//                        "mi-freeform",
//                        modes,
//                        51,
//                        false,
//                        1
//                    )
//
//                    overDisplayHandleObj::class.java.declaredMethods.forEach {
//                        XposedBridge.log(it.toString())
//                    }
//
//                    XposedHelpers.callMethod(
//                        overDisplayHandleObj,
//                        "onWindowCreated",
//                        SurfaceTexture(false),
//                        60.000004f,
//                        16666666L,
//                        2
//                    )
//
//                    XposedBridge.log("success $overDisplayHandleObj")
//
////                                val textureView = TextureView(context!!)
////                                val rootView = LinearLayout(context!!)
////                                rootView.addView(textureView)
////                                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
////                                    override fun onSurfaceTextureSizeChanged(
////                                        surface: SurfaceTexture,
////                                        width: Int,
////                                        height: Int
////                                    ) {
////
////                                    }
////
////                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
////
////                                    }
////
////                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
////                                        surface.release()
////                                        return true
////                                    }
////
////                                    override fun onSurfaceTextureAvailable(
////                                        surface: SurfaceTexture,
////                                        width: Int,
////                                        height: Int
////                                    ) {
////                                        XposedHelpers.callMethod(
////                                            overDisplayHandleObj,
////                                            "onWindowCreated",
////                                            surface,
////                                            60.000004f,
////                                            16666666L,
////                                            2
////                                        )
////                                        XposedBridge.log("hook success onWindowCreated")
////                                    }
////
////                                }
//
//                    param!!.result = "hi world"
//                }
//            }
//        )
//    }
//
//    /**
//     * 修改为叠加的，就不会显示不支持辅助屏
//     */
//    private fun hookVirtualDisplayDevice() {
//        val classLoader = Thread.currentThread().contextClassLoader
//        val clazz = XposedHelpers.findClass("com.android.server.display.VirtualDisplayAdapter\$VirtualDisplayDevice", classLoader)
//
//        XposedHelpers.findAndHookMethod(
//            clazz,
//            "getDisplayDeviceInfoLocked",
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    val displayDeviceInfoClazz = XposedHelpers.findClass("com.android.server.display.DisplayDeviceInfo", classLoader)
//
//                    displayDeviceInfoClazz.getField("type").set(param?.result, 4)
//                }
//            }
//        )
//
//        XposedHelpers.setStaticFloatField(clazz, "REFRESH_RATE", 120.0f)
//    }
//
//    /**
//     * 显示悬浮窗，为了在系统界面也显示
//     */
//    private fun hookPhoneWindowManager() {
//        val classLoader =
//            Thread.currentThread().contextClassLoader
//        val pwm = XposedHelpers.findClass(
//            "com.android.server.policy.PhoneWindowManager",
//            classLoader
//        )
//        /**
//         * xposed模式
//         * @see com.sunshine.freeform.activity.free_form_setting.FreeFormSettingView
//         */
//        //成功获取了ims后，就可以进行hook了
//        XposedHelpers.findAndHookMethod(
//            pwm,
//            "checkAddPermission",
//            Int::class.javaPrimitiveType,
//            Boolean::class.javaPrimitiveType,
//            String::class.java,
//            IntArray::class.java,
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    if (param.args[2].toString() == "com.sunshine.freeform") {
//                        param.result = 0
//                    }
//                }
//            }
//        )
//    }
//
//    private var targetUid = -1
//
//    private fun hookIMS() {
//        val classLoader =
//            Thread.currentThread().contextClassLoader
//        val ims = XposedHelpers.findClass(
//            "com.android.server.input.InputManagerService",
//            classLoader
//        )
//
//        /**
//         * xposed模式
//         * @see com.sunshine.freeform.activity.free_form_setting.FreeFormSettingView
//         */
//        //成功获取了ims后，就可以进行hook了
//        XposedHelpers.findAndHookMethod(
//            ims,
//            "nativeInjectInputEvent",
//            Long::class.javaPrimitiveType,
//            InputEvent::class.java,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam) {
//                    XposedBridge.log(param.args[1].toString())
//                    param.args[3] = 0
//
////                    if (targetUid != -1) {
////                        if (targetUid == param.args[3]) {
////                            param.args[3] = 0
////                        }
////                    } else {
////                        targetUid = context?.packageManager?.getApplicationInfo("com.sunshine.freeform", PackageManager.GET_META_DATA)?.uid?:-1
////                        if (targetUid == param.args[3]) {
////                            param.args[3] = 0
////                        }
////                    }
//                }
//            }
//        )
//    }
//}