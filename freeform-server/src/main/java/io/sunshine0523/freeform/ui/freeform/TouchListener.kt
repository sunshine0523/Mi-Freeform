package io.sunshine0523.freeform.ui.freeform

import android.annotation.SuppressLint
import android.view.Display
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import io.sunshine0523.freeform.service.MiFreeformServiceHolder
import io.sunshine0523.freeform.service.SystemServiceHolder
import kotlin.math.max
import kotlin.math.roundToInt

class MoveTouchListener(
    private val window: FreeformWindow
) : View.OnTouchListener{
    private var startX = 0.0f
    private var startY = 0.0f
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                window.windowManager.updateViewLayout(window.freeformLayout, window.windowParams.apply {
                    x = (x + event.rawX - startX).roundToInt()
                    y = (y + event.rawY - startY).roundToInt()
                })
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                window.makeSureFreeformInScreen()
            }
        }
        return true
    }
}

class LeftViewClickListener(private val window: FreeformWindow) : View.OnClickListener {
    override fun onClick(v: View) {
        window.destroy()
    }

}

/**
 * to full screen
 */
class LeftViewLongClickListener(private val window: FreeformWindow): View.OnLongClickListener {
    override fun onLongClick(v: View): Boolean {
        if (null != window.freeformTaskStackListener) SystemServiceHolder.activityTaskManager.moveRootTaskToDisplay(window.freeformTaskStackListener!!.taskId, Display.DEFAULT_DISPLAY)
        window.destroy(false)
//        FreeformAnimation.toFullScreen(window, 500, object : AnimatorListener {
//            override fun onAnimationEnd(p0: Animator) {
//
//            }
//            override fun onAnimationStart(p0: Animator) {}
//            override fun onAnimationCancel(p0: Animator) {}
//            override fun onAnimationRepeat(p0: Animator) {}
//        })
        return true
    }
}

/**
 * change orientation
 */
class RightViewLongClickListener(private val window: FreeformWindow): View.OnLongClickListener {
    override fun onLongClick(v: View): Boolean {
        window.uiHandler.post {
//            // change orientation
//            val tmp = window.freeformConfig.width
//            window.freeformConfig.width = window.freeformConfig.height
//            window.freeformConfig.height = tmp
//            window.changeOrientation()
            // hangup
            window.handleHangUp()
        }
        return true
    }
}

class RightViewClickListener(private val displayId: Int) : View.OnClickListener {
    override fun onClick(v: View) {
        MiFreeformServiceHolder.back(displayId)
    }
}

class RightScaleTouchListener(private val window: FreeformWindow): View.OnTouchListener {
    private var startX = 0.0f
    private var startY = 0.0f
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                window.freeformConfig.isScaling = true
            }
            MotionEvent.ACTION_MOVE -> {
                window.freeformRootView.layoutParams = window.freeformRootView.layoutParams.apply {
                    width = max(25, (window.freeformRootView.width + event.rawX - startX).roundToInt())
                    height = max(25, (window.freeformRootView.height + event.rawY - startY).roundToInt())
                }
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                window.freeformConfig.isScaling = false
                MiFreeformServiceHolder.resizeFreeform(window, window.freeformConfig.width, window.freeformConfig.height, window.freeformConfig.densityDpi)
//                window.onSurfaceTextureSizeChanged(
//                    window.freeformView.surfaceTexture!!,
//                    window.freeformView.width,
//                    window.freeformView.height
//                )
                //window.uiHandler.post { window.makeSureFreeformInScreen() }
//                MiFreeformServiceHolder.resizeFreeform(
//                    window,
//                    window.freeformConfig.freeformWidth,
//                    window.freeformConfig.freeformHeight,
//                    window.freeformConfig.densityDpi
//                )
            }
        }
        return true
    }
}

class HangUpGestureListener(private val window: FreeformWindow) : SimpleOnGestureListener() {
    private var startX = 0
    private var startY = 0
    override fun onDown(e: MotionEvent): Boolean {
        startX = window.windowParams.x
        startY = window.windowParams.y
        return super.onDown(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        window.uiHandler.post { window.handleHangUp() }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (null == e1) return true
        window.uiHandler.post {
            window.windowManager.updateViewLayout(window.freeformLayout, window.windowParams.apply {
                x = (startX + e2.rawX - e1.rawX).roundToInt()
                y = (startY + e2.rawY - e1.rawY).roundToInt()
            })
        }
        return true
    }
}