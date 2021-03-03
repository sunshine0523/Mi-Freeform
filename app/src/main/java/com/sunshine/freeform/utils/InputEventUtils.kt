package com.sunshine.freeform.utils

import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent

import com.sunshine.freeform.EventData
import com.sunshine.freeform.service.floating.FreeFormConfig

import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/22
 * event工具类
 */
class InputEventUtils {

    companion object {
        private const val TAG = "InputEventUtils"
    }

    private var method: Method? = null
    private var inputManager: InputManager? = null

    private var count: Int = 0
    private var xArray: FloatArray? = null
    private var yArray: FloatArray? = null

    //子线程handler，注入输入时间在主线程会ANR，所以在子线程完成
    private lateinit var handler: Handler

    //为了避免频繁创建对象，这里放到类中
    private lateinit var pointerProperties: Array<MotionEvent.PointerProperties?>
    private lateinit var pointerCoords: Array<MotionEvent.PointerCoords?>
    private lateinit var coords: MotionEvent.PointerCoords
    private lateinit var injectMotionEvent: MotionEvent

    /**
     * xposed注入
     */
    private fun xposedInjectInputEvent(
        inputEvent: InputEvent,
        displayId: Int
    ) {
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(inputEvent, displayId)
        getInjectInputEventMethod()?.invoke(inputManager, inputEvent, 0)
    }

    fun xposedInjectKeyEvent(
        event: KeyEvent,
        displayId: Int
    ) {
        handler.post {
            xposedInjectInputEvent(event, displayId)
        }
    }

    fun testXposedInjectMotionEvent(): Boolean {
        return try {
            val motionEvent = MotionEvent.obtain(0, 0, 0, 0.0f, 0.0f, 0)
            xposedInjectInputEvent(motionEvent, -1)
            true
        }catch (e: Exception) {
            false
        }
    }

    fun xposedInjectMotionEvent(
        event: MotionEvent?,
        displayId: Int,
        scale: Float
    ) {
        if (event == null) return
        count = event.pointerCount
        pointerProperties = arrayOfNulls(count)
        pointerCoords = arrayOfNulls(count)
        for (i in 0 until count) {
            coords = MotionEvent.PointerCoords()
            event.getPointerCoords(i, coords)

            pointerProperties[i] = MotionEvent.PointerProperties()
            pointerProperties[i]!!.id = i
            pointerProperties[i]!!.toolType = MotionEvent.TOOL_TYPE_FINGER

            pointerCoords[i] = MotionEvent.PointerCoords()
            pointerCoords[i]!!.apply {
                orientation = 0f
                pressure = 1f
                size = 1f
                x = coords.x * scale
                y = coords.y * scale
            }
        }

        handler.post {
            injectMotionEvent = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                event.action,
                count,
                pointerProperties,
                pointerCoords,
                event.metaState,
                event.buttonState,
                event.xPrecision,
                event.yPrecision,
                event.deviceId,//eventData.deviceId,
                event.edgeFlags,
                event.source,
                event.flags//eventData.flags
            )
            xposedInjectInputEvent(injectMotionEvent, displayId)
        }
    }

    /**
     * xposed注入，用于全屏应用返回键
     */
    fun xposedInjectInputEventWithoutDisplayId(
        inputEvent: InputEvent,
    ) {
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

    /**
     * root注入motion
     * @param ratio 屏幕和小窗大小比例
     */
    fun rootInjectMotionEvent(
        motionEvent: MotionEvent?,
        displayId: Int,
        scale: Float
    ) {
        if (motionEvent == null) return
        /**
         * 因为这个post的原因，
         * 会导致event延时，
         * 有时遇到looper空闲了，
         * 就会把最新的event插队，
         * 这样就会导致event发送的顺序变化，
         * 从而导致闪屏现象
         * 然后发现，不能用协程，协程会导致序列化有问题
         * 最终采用了在主线程来执行count xArray yArray的处理，可以避免上述问题
         * 现在怀疑，滑动卡顿是因为有的event没有传送过去
         */
        count = motionEvent.pointerCount
        xArray = FloatArray(count)
        yArray = FloatArray(count)

        for (i in 0 until count) {
            val coords = MotionEvent.PointerCoords()
            motionEvent.getPointerCoords(i, coords)
            xArray!![i] = coords.x * scale
            yArray!![i] = coords.y * scale
        }
        FreeFormConfig.handler?.post {
            try {
                FreeFormConfig.touchOos!!.writeObject(EventData(1, motionEvent.action, xArray!!, yArray!!, motionEvent.flags, motionEvent.source, displayId))
                FreeFormConfig.touchOos!!.flush()
            } catch (e: Exception) {
                println("onTouch $e")
                //仅重启连接不移除所有小窗
                FreeFormConfig.onDelete(removeAllFreeForm = false)
                FreeFormConfig.init(null, FreeFormConfig.controlModel)
            }
        }
    }

    /**
     * root注入key
     * 只是发送信号，具体交给服务处理
     */
    fun rootInjectKeyEvent(
        displayId: Int
    ) {
        FreeFormConfig.handler?.post {
            try {
                FreeFormConfig.touchOos!!.writeObject(EventData(2, 0, null, null, 0, 0, displayId))
                FreeFormConfig.touchOos!!.flush()
            } catch (e: Exception) {
                println("onTouch $e")
                //仅重启连接不移除所有小窗
                FreeFormConfig.onDelete(removeAllFreeForm = false)
                FreeFormConfig.init(null, FreeFormConfig.controlModel)
            }
        }
    }

    init {
        Thread {
            Looper.prepare()
            handler = Handler(Looper.myLooper()!!)
            Looper.loop()
        }.start()
    }
}