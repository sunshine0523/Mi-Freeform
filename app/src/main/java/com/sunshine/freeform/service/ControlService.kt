package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.*
import com.sunshine.freeform.IControlService

import com.sunshine.freeform.R
import com.sunshine.freeform.bean.MotionEventBean
import com.sunshine.freeform.callback.IOnRotationChangedListener
import com.sunshine.freeform.systemapi.DisplayManager
import com.sunshine.freeform.systemapi.InputManager
import com.sunshine.freeform.systemapi.ServiceManager
import com.sunshine.freeform.systemapi.SurfaceControl
import com.sunshine.freeform.utils.ShellUtils
import java.lang.StringBuilder

/**
 * @author sunshine
 * @date 2021/3/17
 */
class ControlService : IControlService.Stub() {

    private var inputManager: InputManager? = null
    private var windowManager: com.sunshine.freeform.systemapi.WindowManager? = null
    private var displayManager: DisplayManager? = null

    /**
     * 初始化服务
     */
    @SuppressLint("PrivateApi")
    override fun init(): Boolean {
        val serviceManager = ServiceManager()
        inputManager = serviceManager.inputManager
        windowManager = serviceManager.windowManager
        displayManager = serviceManager.displayManager

//        val application = ActivityThread.currentActivityThread().application
//        println("你好$application")

        return inputManager != null && windowManager != null
    }

    override fun test2(): String {
        val builder = StringBuilder()
        displayManager!!.displayIds.forEach {
            builder.append(it).append(" ")
        }
        return displayManager!!.getDisplayInfo(displayManager!!.displayIds[1]).toString()
    }

    override fun initRotationWatcher(callback: IOnRotationChangedListener): Boolean {
        if (windowManager == null) return false
        windowManager!!.registerRotationWatcher(object : IRotationWatcher.Stub() {
            override fun onRotationChanged(rotation: Int) {
                callback.onRotationChanged(rotation)
            }
        })
        return true
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

    @SuppressLint("ResourceType")
    override fun test(displayId: Int): String {
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
            val application = activityThread.getMethod("getApplication").invoke(currentActivityThread) as Application
            val context = application.createPackageContext("com.sunshine.freeform", Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)

            val view = LayoutInflater.from(context).inflate(R.layout.view_freeform_window_new, null, false)

            val textureView: TextureView = view.findViewById(R.id.texture_view)

//            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
//                override fun onSurfaceTextureSizeChanged(
//                    surface: SurfaceTexture,
//                    width: Int,
//                    height: Int
//                ) {
//
//                }
//
//                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//
//                }
//
//                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                    return true
//                }
//
//                override fun onSurfaceTextureAvailable(
//                    surface: SurfaceTexture,
//                    width: Int,
//                    height: Int
//                ) {
//                    val binder = SurfaceControl.createDisplay("test", false)
//                    SurfaceControl.openTransaction()
//                    SurfaceControl.setDisplaySurface(binder, Surface(surface))
//                    SurfaceControl.setDisplayProjection(binder, 0, Rect(0, 0, 1000, 1000), Rect(0, 0, 1000, 1000))
//                    SurfaceControl.setDisplayLayerStack(binder, 0)
//                    SurfaceControl.closeTransaction()
//
//                    (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).registerDisplayListener(object : DisplayManager.DisplayListener {
//                        override fun onDisplayChanged(displayId: Int) {
//
//                        }
//
//                        override fun onDisplayAdded(displayId: Int) {
//
//                        }
//
//                        override fun onDisplayRemoved(displayId: Int) {
//
//                        }
//
//                    }, null)
//                }
//
//            }

//            val layoutParams = WindowManager.LayoutParams()
//            layoutParams.apply {
//                type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG
//                x = 0
//                y = 0
//                this.width = 100
//                this.height = 200
//                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//            }
//
//            val windowManagerS = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//
//            Looper.prepare()
//            windowManagerS.addView(view, layoutParams)
//            Looper.loop()

            return ""
        } catch (e: Exception) {
            return e.stackTraceToString()
        }

    }

    override fun setDisplaySurface(surface: Surface?) {
        val display = SurfaceControl.createDisplay("mi-freeform", false)
        SurfaceControl.openTransaction()
        SurfaceControl.setDisplaySurface(display, surface)
        SurfaceControl.setDisplayProjection(display, 0, Rect(0, 0, 1000, 1000), Rect(0, 0, 1000, 1000))
        SurfaceControl.setDisplayLayerStack(display, 0)
        SurfaceControl.closeTransaction()
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