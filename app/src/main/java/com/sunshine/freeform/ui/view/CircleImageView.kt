package com.sunshine.freeform.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.sunshine.freeform.R

/**
 * @Author : jiyajie
 * @Time : On 2021/11/23 09:45
 * @Description : CircleImageViewV2
 */
class CircleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {


    private var mRadius: Int = 0
    private val viewOutlineProvider: ViewOutlineProvider by lazy {
        object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val width = width
                val height = height
                outline?.setRoundRect(0, 0, width, height, mRadius.toFloat())
            }

        }
    }
    private var path: Path?
    private var rect: RectF?


    init {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, androidx.appcompat.R.styleable.AppCompatImageView)
        obtainStyledAttributes.let {
            mRadius = 10
            it.recycle()
            path = Path()
            rect = RectF()
            setRound(mRadius)
        }
    }

    //设置圆角图片
    private fun setRound(radius: Int) = apply {
        val isChange = radius != mRadius
        mRadius = radius
        if (mRadius != 0) {
            outlineProvider = viewOutlineProvider
            clipToOutline = true
            val width = width.toFloat()
            val height = height.toFloat()
            rect?.set(0f, 0f, width, height)
            path?.reset()
            rect?.let { path?.addRoundRect(it,mRadius.toFloat(),mRadius.toFloat(),Path.Direction.CW) }
        } else {
            clipToOutline = false
        }

        if (isChange) {
            invalidateOutline()
        }

    }

    override fun draw(canvas: Canvas?){
        var clip = false
        if (mRadius > 0) {
            clip = true
            canvas?.save()
            path?.let { canvas?.clipPath(it) }

        }
        super.draw(canvas)
        if (clip) {
            canvas?.restore()
        }
    }
}