package com.sunshine.freeform.utils

import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author sunshine
 * @date 2021/3/5
 */
class HookTest : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName == "com.sunshine.freeform") {
            XposedHelpers.findAndHookMethod(
                "com.sunshine.freeform.utils.HookFun",
                lpparam.classLoader,
                "hook",
                XC_MethodReplacement.returnConstant(true)
            )
        }
    }
}