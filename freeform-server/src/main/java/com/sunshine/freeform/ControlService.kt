package com.sunshine.freeform

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.*

import com.sunshine.freeform.bean.MotionEventBean
import com.sunshine.freeform.systemapi.InputManager
import com.sunshine.freeform.systemapi.ServiceManager
import com.sunshine.freeform.systemapi.WindowManager
import com.sunshine.freeform.utils.ShellUtils

/**
 * @author sunshine
 * @date 2021/3/17
 */
class ControlService : IControlService.Stub() {

    private var inputManager: InputManager? = null
    private var windowManager: WindowManager? = null

    /**
     * 初始化服务
     */
    @SuppressLint("PrivateApi")
    override fun init(): Boolean {
        val serviceManager = ServiceManager()
        inputManager = serviceManager.inputManager
        windowManager = serviceManager.windowManager

        return inputManager != null && windowManager != null
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

        try {
            InputManager.setDisplayId(motionEvent, motionEventBean.displayId)
            inputManager!!.injectInputEvent(motionEvent, 0)
        } catch (e: Exception) {}

        motionEvent.recycle()
    }

    /**
     * 移动到全屏
     */
    override fun moveStack(displayId: Int): Boolean {
        val stackId: String = ShellUtils.execCommand(
            "am stack list | grep displayId=$displayId",
            true
        ).successMsg.split(" ".toRegex()).toTypedArray()[1].replace("id=", "")

        val result = ShellUtils.execCommand(
            "am display move-stack $stackId 0",
            true
        ).result

        return result == 0
    }

    override fun getRotation(): Int {
        if (windowManager == null) return -1
        return windowManager!!.rotation
    }

    /**
     * 通过adb方式启动一个活动
     */
    override fun startActivity(command: String?): Boolean {
        return ShellUtils.execCommand(command, false).result == 0
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
            InputManager.setDisplayId(down, displayId)
            KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                .invoke(down, InputDevice.SOURCE_KEYBOARD)
            inputManager!!.injectInputEvent(down, 0)

            InputManager.setDisplayId(up, displayId)
            KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                .invoke(up, InputDevice.SOURCE_KEYBOARD)
            inputManager!!.injectInputEvent(up, 0)
        } catch (e: Exception) {

        }
    }
}