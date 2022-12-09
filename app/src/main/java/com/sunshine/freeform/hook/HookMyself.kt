package com.sunshine.freeform.hook

import com.sunshine.freeform.hook.utils.XLog
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookMyself : IXposedHookLoadPackage {
    override fun handleLoadPackage(p0: XC_LoadPackage.LoadPackageParam) {
        if (p0.packageName == "com.sunshine.freeform") {
//            XposedHelpers.findAndHookMethod(
//                "com.sunshine.freeform.ui.permission.PermissionActivity",
//                p0.classLoader,
//                "checkXposedPermission",
//                Boolean::class.javaPrimitiveType,
//                object : XC_MethodHook() {
//                    override fun beforeHookedMethod(param: MethodHookParam) {
//                        param.args[0] = true
//                    }
//                }
//            )
//            XposedHelpers.findAndHookMethod(
//                "com.sunshine.freeform.ui.main.HomeFragment",
//                p0.classLoader,
//                "checkXposedPermission",
//                Boolean::class.javaPrimitiveType,
//                object : XC_MethodHook() {
//                    override fun beforeHookedMethod(param: MethodHookParam) {
//                        param.args[0] = true
//                    }
//                }
//            )
            XposedHelpers.findAndHookMethod(
                "com.sunshine.freeform.hook.utils.HookTest",
                p0.classLoader,
                "checkXposed",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
        }
    }


}