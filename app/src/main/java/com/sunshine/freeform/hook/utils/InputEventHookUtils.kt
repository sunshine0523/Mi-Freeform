package com.sunshine.freeform.hook.utils

import android.hardware.input.InputManager
import android.view.InputEvent

import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/22
 * event工具类
 */
object InputEventHookUtils {

    private var method: Method? = null
    private var inputManager: InputManager? = null

    /**
     * xposed注入
     */
    fun xposedInjectInputEvent(
        inputEvent: InputEvent,
        displayId: Int
    ) {
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(inputEvent, displayId)
        getInjectInputEventMethod()?.invoke(inputManager, inputEvent, 0)
    }

    /**
     * xposed注入 反射获取方法
     */
    private fun getInjectInputEventMethod(): Method? {
        if (method != null && inputManager != null) return method
        val clazz = InputManager::class.java
        return try {
            inputManager = clazz.getMethod("getInstance").invoke(clazz) as InputManager
            method = clazz.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            method
        } catch (e: Exception) {
            e.fillInStackTrace().printStackTrace()
            null
        }
    }
}