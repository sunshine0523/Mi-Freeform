package com.sunshine.freeform.utils

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author sunshine
 * @date 2021/2/28
 * 经过排查，小窗内旋转屏幕系统UI闪退是因为DisplayLayout中调用set方法会为空，那么，就将上次的参数保存，如果遇到空，就设置给这个
 */
class HookSystemUI : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        //只有android 11上才有
        if (android.os.Build.VERSION.SDK_INT >= 30 && lpparam?.packageName == "com.android.systemui") {
            val clazz = XposedHelpers.findClass("com.android.systemui.wm.DisplayLayout", lpparam.classLoader)
            var lastObj: Any? = null
            XposedBridge.hookAllMethods(clazz, "set", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    if (param != null) {
                        val obj = param.args[0]
                        if (obj != null) {
                            lastObj = obj
                        } else {
                            param.args[0] = lastObj
                        }
                    }
                }
            })

//            val clazz1 = XposedHelpers.findClass("com.android.systemui.SystemUI", lpparam!!.classLoader)
//            XposedHelpers.findAndHookConstructor(
//                clazz1,
//                Context::class.java,
//                object : XC_MethodHook() {
//                    override fun afterHookedMethod(param: MethodHookParam?) {
//                        println(param!!.args[0])
//                    }
//                }
//            )
        }



        if (android.os.Build.VERSION.SDK_INT == 29 && lpparam?.packageName == "com.android.systemui") {
            val clazz = XposedHelpers.findClass("com.android.systemui.SystemBars", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                clazz,
                "createStatusBarFromConfig",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
//                        val superClazz = XposedHelpers.findClass("com.android.systemui.SystemUI", lpparam.classLoader)
//                        val context = XposedHelpers.findField(superClazz, "mContext").get(param!!.thisObject) as Context
//                        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//                        val button = Button(context)
//                        val layoutParams = WindowManager.LayoutParams()
//                        layoutParams.width = 1000
//                        layoutParams.height = 1000
//                        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//                        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
//                        windowManager.addView(button, layoutParams)
//
//                        val userServiceArgs = Shizuku.UserServiceArgs(ComponentName("com.android.systemui", MyAidl::class.java.name))

//                        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
//                        val method = inputManager::class.java.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
//                        val motionEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0.0f, 0.0f, 0)
//                        motionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(motionEvent, 1)
//                        method.invoke(inputManager, motionEvent, 0)
//
//                        val activityOptions = ActivityOptions.makeBasic()
//                        activityOptions.launchDisplayId = 0
//                        val packageManager = context.packageManager
//                        val intent = packageManager.getLaunchIntentForPackage("com.sunshine.lnuplus")
//                        if (intent != null) {
//                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), activityOptions.toBundle())
//                        }
                    }
                }
            )
        }
    }
}