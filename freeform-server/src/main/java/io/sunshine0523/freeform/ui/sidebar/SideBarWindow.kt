package io.sunshine0523.freeform.ui.sidebar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.view.Display
import android.view.IRotationWatcher
import android.view.View
import android.view.WindowManager
import android.view.WindowManagerHidden
import io.sunshine0523.freeform.service.SystemServiceHolder
import io.sunshine0523.freeform.util.MLog
/**
 * @author KindBrave
 * @since 2023/8/22
 */
class SideBarWindow(
    val context: Context,
    val uiHandler: Handler
) : IRotationWatcher.Stub() {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val leftWindowParams = WindowManagerHidden.LayoutParams()
    private val rightWindowParams = WindowManagerHidden.LayoutParams()
    lateinit var leftView: View
    lateinit var rightView: View

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
            leftView = View(context)
            rightView = View(context)
            leftView.setBackgroundColor(Color.TRANSPARENT)
            rightView.setBackgroundColor(Color.TRANSPARENT)
            SideBarTouchListener(this)
            leftWindowParams.apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                width = 100
                height = screenHeight / 5
                x = -screenWidth / 2
                y = -screenHeight / 6
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                privateFlags = WindowManagerHidden.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_USE_BLAST or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
                format = PixelFormat.RGBA_8888
            }
            rightWindowParams.apply {
                type = 2026
                width = 100
                height = screenHeight / 5
                x = screenWidth / 2
                y = -screenHeight / 6
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                privateFlags = WindowManagerHidden.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_USE_BLAST or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
                format = PixelFormat.RGBA_8888
            }
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