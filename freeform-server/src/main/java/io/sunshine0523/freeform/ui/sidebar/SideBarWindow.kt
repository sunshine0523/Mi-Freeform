package io.sunshine0523.freeform.ui.sidebar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Display
import android.view.IRotationWatcher
import android.view.WindowManager
import io.sunshine0523.freeform.service.SystemServiceHolder
import io.sunshine0523.freeform.util.MLog
import java.lang.reflect.Field

/**
 * @author KindBrave
 * @since 2023/8/22
 */
class SideBarWindow(
    val context: Context,
    val uiHandler: Handler
) : IRotationWatcher.Stub() {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val leftWindowParams = WindowManager.LayoutParams()
    private val rightWindowParams = WindowManager.LayoutParams()
    lateinit var leftView: SideBarView
    lateinit var rightView: SideBarView

    companion object {
        private const val TAG = "Mi-Freeform/SideBarWindow"
    }

    init {
        addSideBarView()
        SystemServiceHolder.windowManager.watchRotation(this, Display.DEFAULT_DISPLAY)
    }

    override fun onRotationChanged(rotation: Int) {
        destroy()
        addSideBarView(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSideBarView(needUpdateColor: Boolean = true) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        uiHandler.post {
            leftView = SideBarView(context)
            rightView = SideBarView(context)
            leftView.setBackgroundColor(Color.TRANSPARENT)
            rightView.setBackgroundColor(Color.TRANSPARENT)
            SideBarTouchListener(this)
            leftWindowParams.apply {
                type = 2038
                width = 50
                height = screenHeight / 5
                x = -screenWidth / 2
                y = -screenHeight / 6
                flags = 8 or 256 or 1024 or 524288 or 16777216
                format = PixelFormat.RGBA_8888
            }
            rightWindowParams.apply {
                type = 2038
                width = 50
                height = screenHeight / 5
                x = screenWidth / 2
                y = -screenHeight / 6
                flags = 8 or 256 or 1024 or 524288 or 16777216
                format = PixelFormat.RGBA_8888
            }
            setPrivateFlag()
            uiHandler.post {
                runCatching {
                    windowManager.addView(leftView, leftWindowParams)
                    windowManager.addView(rightView, rightWindowParams)

                    if (needUpdateColor) {
                        updateSideBarColor(Color.BLUE)
                        Thread {
                            Thread.sleep(3000L)
                            uiHandler.post { runCatching { updateSideBarColor(Color.TRANSPARENT) } }
                        }.start()
                    }
                }.onFailure {
                    MLog.e(TAG, "$it")
                }
            }
        }
    }

    private fun setPrivateFlag() {
        val classname = "android.view.WindowManager\$LayoutParams"
        try {
            val layoutParamsClass: Class<*> = Class.forName(classname)
            val privateFlags: Field = layoutParamsClass.getField("privateFlags")
            var privateFlagsValue: Int = privateFlags.getInt(leftWindowParams)
            privateFlagsValue = privateFlagsValue or 16
            privateFlags.setInt(leftWindowParams, privateFlagsValue)
            privateFlags.setInt(rightWindowParams, privateFlagsValue)
        } catch (e: Exception) { }
    }

    /**
     * Called in uiHandler
     */
    private fun updateSideBarColor(color: Int) {
        leftView.setBackgroundColor(color)
        rightView.setBackgroundColor(color)
        windowManager.updateViewLayout(leftView, leftWindowParams)
        windowManager.updateViewLayout(rightView, rightWindowParams)
    }

    fun destroy() {
        SystemServiceHolder.windowManager.removeRotationWatcher(this)
        uiHandler.post {
            runCatching {
                windowManager.removeViewImmediate(leftView)
                windowManager.removeViewImmediate(rightView)
            }
        }
    }
}