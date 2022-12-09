package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.*
import com.sunshine.freeform.IControlService

import com.sunshine.freeform.bean.MotionEventBean
import com.sunshine.freeform.systemapi.InputManager
import com.sunshine.freeform.systemapi.ServiceManager
import com.sunshine.freeform.utils.ShellUtils
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * @author sunshine
 * @date 2021/3/17
 */

class ControlService : IControlService.Stub() {
    private var inputManager: InputManager? = null

    /**
     * 初始化服务
     */
    @SuppressLint("PrivateApi")
    override fun init(): Boolean {
        val serviceManager = ServiceManager()
        inputManager = serviceManager.getInputManager()

        return inputManager != null
    }

    override fun execShell(command: String, useRoot: Boolean): Boolean {
        return ShellUtils.execCommand(command, useRoot).result == 0
    }

    /**
     * 触摸事件
     */
    override fun touch(motionEventBean: MotionEventBean) {
        val count = motionEventBean.xArray.size
        val pointerProperties: Array<MotionEvent.PointerProperties?> = arrayOfNulls(count)
        val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(count)
        for (i in 0 until count) {
            pointerProperties[i] = MotionEvent.PointerProperties()
            pointerProperties[i]!!.id = i
            pointerProperties[i]!!.toolType = MotionEvent.TOOL_TYPE_FINGER

            pointerCoords[i] = MotionEvent.PointerCoords()
            pointerCoords[i]!!.apply {
                orientation = 0f
                pressure = 1f
                size = 1f
                x = motionEventBean.xArray[i]
                y = motionEventBean.yArray[i]
            }
        }

        val motionEvent = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            motionEventBean.action,
            count,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1.0f,
            1.0f,
            -1,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )

        inputManager!!.injectInputEvent(motionEvent, motionEventBean.displayId)

        motionEvent.recycle()
    }

    /**
     * 移动到全屏
     */
    override fun moveStack(displayId: Int): Boolean {
        return try {
            val stackId: String = ShellUtils.execCommand(
                "am stack list | grep displayId=$displayId",
                false
            ).successMsg!!.split(" ".toRegex()).toTypedArray()[1].replace("id=", "")

            val result = ShellUtils.execCommand(
                "am display move-stack $stackId 0",
                false
            ).result

            result == 0
        } catch (e: Exception) {
            false
        }

    }

    /**
     * 点击返回
     */
    override fun pressBack(displayId: Int) {
        val down = KeyEvent(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_BACK,
            0
        )
        val up = KeyEvent(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_BACK,
            0
        )

        try {
            KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                .invoke(down, InputDevice.SOURCE_KEYBOARD)
            inputManager!!.injectInputEvent(down, displayId)

            KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                .invoke(up, InputDevice.SOURCE_KEYBOARD)
            inputManager!!.injectInputEvent(up, displayId)
        } catch (e: Exception) {

        }
    }
}