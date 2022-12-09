package com.sunshine.freeform.ui.view

import android.content.Context
import android.util.AttributeSet

/**
 * @date 2022/8/23
 * @author sunshine0523
 * 获得焦点以自动跑马灯
 */
class MTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    override fun isFocused(): Boolean {
        return true
    }
}