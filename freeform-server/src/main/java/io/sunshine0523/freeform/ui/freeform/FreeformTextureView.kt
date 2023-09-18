package io.sunshine0523.freeform.ui.freeform

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView

/**
 * @author KindBrave
 * @since 2023/9/16
 */
class FreeformTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TextureView(context, attrs) {

    companion object {
        private const val TAG = "Mi-Freeform/FreeformTextureView"
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericPointerEvent(event: MotionEvent?): Boolean {
        return super.dispatchGenericPointerEvent(event)
    }
}