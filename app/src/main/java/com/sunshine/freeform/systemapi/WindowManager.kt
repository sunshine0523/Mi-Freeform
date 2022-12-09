package com.sunshine.freeform.systemapi

import android.os.IInterface
import android.view.IRotationWatcher
import java.lang.AssertionError
import java.lang.Exception

class WindowManager(private val manager: IInterface) {
    // method changed since this commit:
    // https://android.googlesource.com/platform/frameworks/base/+/8ee7285128c3843401d4c4d0412cd66e86ba49e3%5E%21/#F2
    val rotation: Int
        get() = try {
            val cls: Class<*> = manager.javaClass
            try {
                manager.javaClass.getMethod("getRotation").invoke(manager) as Int
            } catch (e: NoSuchMethodException) {
                // method changed since this commit:
                // https://android.googlesource.com/platform/frameworks/base/+/8ee7285128c3843401d4c4d0412cd66e86ba49e3%5E%21/#F2
                cls.getMethod("getDefaultDisplayRotation").invoke(manager) as Int
            }
        } catch (e: Exception) {
            throw AssertionError(e)
        }

    fun registerRotationWatcher(rotationWatcher: IRotationWatcher?) {
        try {
            val cls: Class<*> = manager.javaClass
            try {
                cls.getMethod("watchRotation", IRotationWatcher::class.java)
                    .invoke(manager, rotationWatcher)
            } catch (e: NoSuchMethodException) {
                // display parameter added since this commit:
                // https://android.googlesource.com/platform/frameworks/base/+/35fa3c26adcb5f6577849fd0df5228b1f67cf2c6%5E%21/#F1
                cls.getMethod(
                    "watchRotation", IRotationWatcher::class.java, Int::class.javaPrimitiveType
                ).invoke(manager, rotationWatcher, 0)
            }
        } catch (e: Exception) {
            throw AssertionError(e)
        }
    }
}