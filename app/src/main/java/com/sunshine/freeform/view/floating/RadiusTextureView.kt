package com.sunshine.freeform.view.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import android.graphics.Rect


/**
 * @author sunshine
 * @date 2022/1/8
 */
class RadiusTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TextureView(context, attrs) {

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val rect = Rect(0, 0, view!!.measuredWidth, view.measuredHeight)
                outline?.setRoundRect(rect, 32.0f)
            }
        }
        clipToOutline = true
    }
}