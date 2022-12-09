package com.sunshine.freeform.systemapi

import android.annotation.SuppressLint
import android.os.IInterface
import android.view.InputEvent
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/10
 */
@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
class InputManager(private val manager: IInterface) {

    private var injectInputEventMethod: Method? = null
    private var setDisplayIdMethod: Method? = null

    private fun getInjectInputEventMethod(): Method? {
        try {
            if (injectInputEventMethod == null) {
                injectInputEventMethod = manager.javaClass.getMethod(
                    "injectInputEvent",
                    InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
        return injectInputEventMethod
    }

    fun injectInputEvent(inputEvent: InputEvent, displayId: Int): Boolean {
        setDisplayId(inputEvent, displayId)
        return try {
            val method = getInjectInputEventMethod()
            method!!.invoke(manager, inputEvent, 0) as Boolean
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            false
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            false
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 反射获取setDisplayId(int)方法
     */
    private fun getSetDisplayIdMethod(): Method? {
        if (setDisplayIdMethod == null) {
            setDisplayIdMethod = InputEvent::class.java.getMethod(
                "setDisplayId",
                Int::class.javaPrimitiveType
            )
        }
        return setDisplayIdMethod
    }

    /**
     * 设置displayId
     * 将副屏id传入
     * 用于控制副屏
     */
    private fun setDisplayId(inputEvent: InputEvent, displayId: Int): Boolean {
        return try {
            val method =
                getSetDisplayIdMethod()
            method?.invoke(inputEvent, displayId)
            true
        }catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}