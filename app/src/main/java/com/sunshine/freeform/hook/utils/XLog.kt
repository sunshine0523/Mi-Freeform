package com.sunshine.freeform.hook.utils

import de.robv.android.xposed.XposedBridge

object XLog {

    fun d(s: Any) {
        XposedBridge.log("[Mi-Freeform/D] $s")
    }

    fun e(s: Any) {
        XposedBridge.log("[Mi-Freeform/E] $s")
    }
}