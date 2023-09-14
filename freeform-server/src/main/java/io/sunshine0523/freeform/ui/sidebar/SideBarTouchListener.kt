package io.sunshine0523.freeform.ui.sidebar

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.view.MotionEvent
import android.view.View
import io.sunshine0523.freeform.util.MLog

/**
 * @author KindBrave
 * @since 2023/8/23
 */
@SuppressLint("ClickableViewAccessibility")
class SideBarTouchListener(private val sideBarWindow: SideBarWindow) {
    companion object {
        private const val TAG = "Mi-Freeform/SideBarTouchListener"
    }
    init {
        sideBarWindow.uiHandler.post {
            val leftGestureManager = MGestureManager(sideBarWindow.context, LeftListener())
            val rightGestureManager = MGestureManager(sideBarWindow.context, RightListener())
            val leftTouchListener = View.OnTouchListener { _, e ->
                leftGestureManager.onTouchEvent(e)
                true
            }
            val rightTouchListener = View.OnTouchListener { _, e ->
                rightGestureManager.onTouchEvent(e)
                true
            }
            sideBarWindow.leftView.setOnTouchListener(leftTouchListener)
            sideBarWindow.rightView.setOnTouchListener(rightTouchListener)
        }
    }

    inner class LeftListener : MGestureManager.MGestureListener {
        override fun singleFingerSlipAction(
            gestureEvent: MGestureManager.GestureEvent?,
            startEvent: MotionEvent?,
            endEvent: MotionEvent?,
            velocity: Float
        ): Boolean {
            if (null != gestureEvent) {
                val intent = Intent().apply {
                    component = ComponentName("com.sunshine.freeform", "com.sunshine.freeform.ui.floating.FloatingActivity")
                    putExtra("isLeft", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { sideBarWindow.context.startActivity(intent) }.onFailure { MLog.e(TAG, "$it") }
            }
            return true
        }
    }

    inner class RightListener : MGestureManager.MGestureListener {
        override fun singleFingerSlipAction(
            gestureEvent: MGestureManager.GestureEvent?,
            startEvent: MotionEvent?,
            endEvent: MotionEvent?,
            velocity: Float
        ): Boolean {
            if (null != gestureEvent) {
                when (gestureEvent) {
                    MGestureManager.GestureEvent.SINGLE_GINGER_LEFT_SLIP, MGestureManager.GestureEvent.SINGLE_GINGER_RIGHT_SLIP -> {
                        val intent = Intent().apply {
                            component = ComponentName("com.sunshine.freeform", "com.sunshine.freeform.ui.floating.FloatingActivity")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            action = Intent.ACTION_MAIN
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            putExtra("isLeft", false)
                        }
                        runCatching { sideBarWindow.context.startActivity(intent) }.onFailure { MLog.e(TAG, "$it") }
                        return true
                    }
                    else -> { }
                }
            }
            return false
        }
    }
}