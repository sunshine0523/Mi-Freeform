package com.sunshine.freeform.hook.view

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.view.InputEvent
import com.sunshine.freeform.hook.utils.HookFailException

/**
 * @author sunshine
 * @date 2021/8/1
 */
abstract class FreeFormHookWindowAbs {
    abstract val context: Context
    abstract val packageName: String
    abstract val command: String
    abstract val userId: Int
    abstract var displayId: Int

    abstract fun show()

    abstract fun resize()

    abstract fun addOverlayDevice(
        surfaceTexture: SurfaceTexture,
        freeFormCount: Int,
        displayName: String,
        screenWidth: Int,
        screenHeight: Int,
        dpi: Int
    )

    abstract fun updateViewLayout()

    abstract fun injectEvent(inputEvent: InputEvent, displayId: Int)

    abstract fun startActivity(command: String) : Boolean

    abstract fun moveToTop() : Boolean

    abstract fun resizeFreeForm(movedX: Float, movedY: Float, position: Int)

    abstract fun setWidthHeight()

    abstract fun exitSmall()

    abstract fun removeView()

    abstract fun killApp(packageName: String)

    abstract fun destroy()

    protected fun Float.dp2px(): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}